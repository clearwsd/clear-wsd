package edu.colorodo.clear.wsd.feature.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import edu.colorodo.clear.wsd.feature.resource.MultimapResource;

/**
 * TSV resource initializer used to initialize a {@link MultimapResource} from an {@link InputStream}.
 *
 * @author jamesgung
 */
public class TsvResourceInitializer<K> implements BiConsumer<MultimapResource<K>, InputStream> {

    @Override
    public void accept(MultimapResource<K> resource, InputStream inputStream) {
        ListMultimap<String, String> multimap = ArrayListMultimap.create();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> fields = Arrays.asList(line.split("\t"));
                String key = resource.keyFunction().apply(fields.get(0));
                fields.subList(1, fields.size()).stream()
                        .map(s -> resource.valueFunction().apply(s))
                        .forEach(s -> multimap.put(key, s));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error initializing TSV resource.", e);
        }
        resource.multimap(ImmutableListMultimap.copyOf(multimap));
    }
}