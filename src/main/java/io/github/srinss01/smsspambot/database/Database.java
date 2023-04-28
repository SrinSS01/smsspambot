package io.github.srinss01.smsspambot.database;

import io.github.srinss01.smsspambot.Config;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Getter
public class Database {
    Config config;
    ActivationRepo activationRepo;
}
