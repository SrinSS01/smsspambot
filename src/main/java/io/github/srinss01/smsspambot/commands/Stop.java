package io.github.srinss01.smsspambot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

public class Stop extends CommandDataImpl implements ICustomCommandData {
    public static final Stop COMMAND = new Stop();
    public Stop() {
        super("stop", "Stops the bot");
        setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR));
    }

    @Override
    public void execute(SlashCommandInteraction interaction) {
        interaction.reply("Stopping bot...").setEphemeral(true).queue(it -> it.getJDA().shutdownNow());
    }
}
