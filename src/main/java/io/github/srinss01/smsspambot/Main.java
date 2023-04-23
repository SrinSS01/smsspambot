package io.github.srinss01.smsspambot;

import io.github.srinss01.smsspambot.database.Database;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import org.slf4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.swing.*;

@Component
@AllArgsConstructor
public class Main implements CommandLineRunner {
    final Database database;
    final Events events;
    // logger
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Main.class);

    @Override
    public void run(String... args) {
        Config config = database.getConfig();
        String token = config.getToken();
        boolean headless = SMSSpamBotApplication.isHeadless();
        if (token == null || token.isEmpty()) {
            if (!headless) {
                JOptionPane.showMessageDialog(null, "Token is empty", "Error", JOptionPane.ERROR_MESSAGE);
            }
            logger.error("Token is empty");
            return;
        }
        logger.info("Starting bot with token: {}", token);
        try {
            JDABuilder
                    .createDefault(token)
                    .addEventListeners(events).build();
        } catch (InvalidTokenException e) {
            if (headless) {
                throw e;
            } else JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + token, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
