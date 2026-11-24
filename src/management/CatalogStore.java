package management;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import management.Catalog.AdStatus;
import management.Catalog.Advertisement;
import management.Catalog.Product;
import management.Catalog.ProductStatus;

final class CatalogStore implements AutoCloseable {
    private final Path path;
    private final FileChannel lockChannel;
    private Catalog catalog;

    CatalogStore(Path path) throws IOException {
        this.path = path.toAbsolutePath().normalize();
        Files.createDirectories(this.path.getParent());
        Path lockPath = this.path.resolveSibling(this.path.getFileName() + ".lock");
        lockChannel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock = lockChannel.tryLock();
            if (lock == null) {
                throw new IOException("같은 데이터 파일을 사용하는 프로그램이 이미 열려 있습니다.");
            }
            catalog = load();
        } catch (OverlappingFileLockException exception) {
            close();
            throw new IOException("같은 데이터 파일을 사용하는 프로그램이 이미 열려 있습니다.", exception);
        } catch (IOException | RuntimeException exception) {
            close();
            throw exception;
        }
    }

    Catalog catalog() {
        return catalog;
    }

    void save(Catalog updated) throws IOException {
        Properties data = new Properties();
        data.setProperty("version", "1");
        data.setProperty("product.count", Integer.toString(updated.products().size()));
        data.setProperty("ad.count", Integer.toString(updated.advertisements().size()));
        for (int index = 0; index < updated.products().size(); index++) {
            Product item = updated.products().get(index);
            String key = "product." + index + ".";
            data.setProperty(key + "id", item.id().toString());
            data.setProperty(key + "name", item.name());
            data.setProperty(key + "code", item.code());
            data.setProperty(key + "price", Long.toString(item.price()));
            data.setProperty(key + "stock", Integer.toString(item.stock()));
            data.setProperty(key + "status", item.status().name());
        }
        for (int index = 0; index < updated.advertisements().size(); index++) {
            Advertisement item = updated.advertisements().get(index);
            String key = "ad." + index + ".";
            data.setProperty(key + "id", item.id().toString());
            data.setProperty(key + "productId", item.productId().toString());
            data.setProperty(key + "name", item.name());
            data.setProperty(key + "channel", item.channel());
            data.setProperty(key + "budget", Long.toString(item.budget()));
            data.setProperty(key + "start", item.start().toString());
            data.setProperty(key + "end", item.end().toString());
            data.setProperty(key + "status", item.status().name());
        }

        Path temporary = Files.createTempFile(path.getParent(), "catalog-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                data.store(writer, "Product and advertisement catalog");
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            catalog = updated;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private Catalog load() throws IOException {
        if (Files.notExists(path)) {
            return new Catalog(List.of(), List.of());
        }

        Properties data = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            data.load(reader);
            if (!"1".equals(data.getProperty("version"))) {
                throw new IllegalArgumentException("Unsupported catalog version");
            }

            List<Product> products = new ArrayList<>();
            int productCount = count(data, "product.count");
            for (int index = 0; index < productCount; index++) {
                String key = "product." + index + ".";
                products.add(new Product(UUID.fromString(value(data, key + "id")), value(data, key + "name"),
                        value(data, key + "code"), Long.parseLong(value(data, key + "price")),
                        Integer.parseInt(value(data, key + "stock")), ProductStatus.valueOf(value(data, key + "status"))));
            }

            List<Advertisement> advertisements = new ArrayList<>();
            int adCount = count(data, "ad.count");
            for (int index = 0; index < adCount; index++) {
                String key = "ad." + index + ".";
                advertisements.add(new Advertisement(UUID.fromString(value(data, key + "id")),
                        UUID.fromString(value(data, key + "productId")), value(data, key + "name"),
                        value(data, key + "channel"), Long.parseLong(value(data, key + "budget")),
                        LocalDate.parse(value(data, key + "start")), LocalDate.parse(value(data, key + "end")),
                        AdStatus.valueOf(value(data, key + "status"))));
            }
            return new Catalog(products, advertisements);
        } catch (RuntimeException exception) {
            throw new IOException("저장된 파일을 읽을 수 없습니다.\n" + path, exception);
        }
    }

    private static int count(Properties data, String key) {
        int count = Integer.parseInt(value(data, key));
        if (count < 0) {
            throw new IllegalArgumentException("Invalid count: " + key);
        }
        return count;
    }

    private static String value(Properties data, String key) {
        String value = data.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        return value;
    }

    @Override
    public void close() throws IOException {
        lockChannel.close();
    }
}
