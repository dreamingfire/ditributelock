package ml.dreamingfire.group.test.distributelock.util;

import java.io.*;
import java.util.Objects;
import java.util.Properties;

public final class PropertyFileUtil {
    private static Properties properties = new Properties();

    static {
        System.out.println("loading env properties");
        try {
            Reader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    Objects.requireNonNull(PropertyFileUtil.class.getClassLoader().getResourceAsStream("env.properties"))
                            )
                    );
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return properties.getProperty(key, "");
    }
}
