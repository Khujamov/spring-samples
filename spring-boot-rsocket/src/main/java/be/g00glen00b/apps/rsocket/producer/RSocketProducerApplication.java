package be.g00glen00b.apps.rsocket.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.BaseStream;

@Slf4j
@SpringBootApplication
public class RSocketProducerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RSocketProducerApplication.class, args);
    }

    @Bean
    public ApplicationRunner seeder(DatabaseClient client, PersonRepository repository) {
        return args -> getSchema()
            .flatMap(sql -> executeSql(client, sql))
            .doOnSuccess(count -> log.info("Schema created"))
            .then(repository.deleteAll())
            .doOnSuccess(v -> log.info("Repository cleared"))
            .thenMany(getPeople())
            .flatMap(repository::save)
            .subscribe(person -> log.info("New person‍: {}", person), null, () -> log.info("🚀 Data generated"));
    }

    private Flux<Person> getPeople() {
        return Flux.just(new Person(null, "John", "Doe"), new Person(null, "Jane", "Doe"));
    }

    private Mono<Integer> executeSql(DatabaseClient client, String sql) {
        return client.execute().sql(sql).fetch().rowsUpdated();
    }

    private Mono<String> getSchema() throws URISyntaxException {
        Path path = Paths.get(ClassLoader.getSystemResource("schema.sql").toURI());
        return Flux
            .using(() -> Files.lines(path), Flux::fromStream, BaseStream::close)
            .reduce((line1, line2) -> line1 + "\n" + line2);
    }
}
