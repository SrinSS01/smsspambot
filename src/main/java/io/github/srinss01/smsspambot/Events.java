package io.github.srinss01.smsspambot;

import io.github.srinss01.smsspambot.commands.CommandsCollection;
import io.github.srinss01.smsspambot.commands.ICustomCommandData;
import io.github.srinss01.smsspambot.commands.Spam;
import io.github.srinss01.smsspambot.commands.Stop;
import io.github.srinss01.smsspambot.database.Database;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
public class Events extends ListenerAdapter {
    Database database;
    Spam spamCommand;
    private final CommandsCollection commandsCollection = new CommandsCollection();
    // logger
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Events.class);

    public Events(Database database) {
        this.database = database;
        put(Stop.COMMAND);
        spamCommand = Spam.COMMAND.apply(database);
        put(spamCommand);
    }

    private void put(ICustomCommandData commandData) {
        commandsCollection.put(commandData.getName(), commandData);
    }
    @Override
    public void onReady(ReadyEvent event) {
        logger.info("{} is ready.", event.getJDA().getSelfUser().getName());
        event.getJDA().updateCommands().addCommands(commandsCollection.values()).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        commandsCollection.get(event.getName()).execute(event);
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        if (modalId.contains("spam")) {
            spamCommand.spamModalInteraction(event);
        } else if (modalId.equals("activation")) {
            spamCommand.activationModalInteraction(event);
        }
    }
}
