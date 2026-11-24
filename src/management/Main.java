package management;

import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] arguments) {
        Path dataPath = arguments.length == 0 ? Path.of("data", "catalog.properties") : Path.of(arguments[0]);
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            try {
                CatalogStore store = new CatalogStore(dataPath);
                new ManagementFrame(store).setVisible(true);
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(null, exception.getMessage(), "프로그램을 열 수 없습니다",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
