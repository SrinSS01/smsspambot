package io.github.srinss01.smsspambot.database;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivationRepo extends JpaRepository<Activations, Long> {}
