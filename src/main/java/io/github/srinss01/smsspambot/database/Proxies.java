package io.github.srinss01.smsspambot.database;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @ToString
@AllArgsConstructor
@NoArgsConstructor
public class Proxies {
    @Id private long id;
    private String proxy;
}
