package io.github.srinss01.smsspambot.database;

import io.github.srinss01.smsspambot.Config;
import io.github.srinss01.smsspambot.auth.ActivationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
@Getter
public class Database {
    Config config;
    public static final Map<Long, String> activationKeyMap = new HashMap<>();
    public static final Map<Long, ActivationStatus> activationSessionMap = new HashMap<>();
}
