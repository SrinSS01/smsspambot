package io.github.srinss01.smsspambot;

import io.github.srinss01.smsspambot.auth.ActivationStatus;
import lombok.Getter;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

@SuppressWarnings({"SameParameterValue"})
@SpringBootApplication
public class SMSSpamBotApplication {
    // logger
    private static final Logger logger = LoggerFactory.getLogger(SMSSpamBotApplication.class);
    @Getter
    private static final boolean headless = GraphicsEnvironment.isHeadless();
    private static final Scanner scanner = new Scanner(System.in);
    @Getter
    private static final List<String> SITES = new ArrayList<>();

    static {
        ActivationStatus.init();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }
        val config = new File("config");
        val sites = new File("sites");
        if (!sites.exists()) {
            val mkdir = sites.mkdir();
            if (!mkdir) {
                logger.error("Failed to create sites directory");
                System.exit(1);
            }
            logger.info("Created sites directory");
        }
        if (!config.exists()) {
            val mkdir = config.mkdir();
            if (!mkdir) {
                logger.error("Failed to create config directory");
                System.exit(1);
            }
            logger.info("Created config directory");
        }
        val properties = new File("config/application.yml");
        if (!properties.exists()) {
            // get environment variables
            var token = Objects.requireNonNullElse(validateEnv("TOKEN"), ask("Enter bot token: ", "Bot Token"));
            var proxy = Objects.requireNonNullElse(validateEnv("PROXY"), ask("Enter proxy: ", "Proxy"));
            scanner.close();
            Config _config = new Config();
            _config.setToken(token);
            _config.setProxy(proxy);
            try {
				Files.writeString(properties.toPath(), _config.toString());
            } catch (IOException e) {
                if (!headless) {
                    JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                logger.error("Error while saving config", e);
                System.exit(1);
            }
			logger.info("Created application.yml file");
        }
        // iterate over all files in sites directory
        for (val file : Objects.requireNonNull(sites.listFiles())) {
            if (file.isFile()) {
                try {
                    SITES.add(Files.readString(file.toPath()));
                } catch (IOException e) {
                    logger.error("Error while reading file {}", file.getName(), e);
                }
            }
        }
        logger.info("Loaded {} sites", SITES.size());
    }

    public static void main(String[] args) {
		new SpringApplicationBuilder(SMSSpamBotApplication.class).headless(headless).run(args);
    }

    private static String validateEnv(String keyName) {
        logger.info("searching for env({})...", keyName);
        var keyVal = System.getenv(keyName);
        if (keyVal == null) {
            logger.info("{} not found, skipping...", keyName);
        } else {
            logger.info("{} found, continuing...", keyName);
            return keyVal;
        }
        return null;
    }

    private static String ask(String message, String title) {
        if (headless) {
            System.out.print(message);
            return scanner.nextLine();
        } else {
            return JOptionPane.showInputDialog(
                    null,
                    message,
                    title,
                    JOptionPane.PLAIN_MESSAGE
            );
        }
    }
}
