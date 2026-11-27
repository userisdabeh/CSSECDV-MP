package View;

import Controller.AccessControl;
import Controller.ErrorHandler;
import Controller.SQLite;
import Controller.Validator;
import Model.History;
import Model.Product;
import Model.User;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.HierarchyEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

abstract class RoleFeatures extends JPanel {
    private static final int MAX_STOCK_LENGTH = 5;
    private static final int MAX_PRICE_LENGTH = 32;

    private final String panelName;
    private final boolean customer;
    private final boolean canViewHistory;
    private final JTabbedPane tabs = new JTabbedPane();
    private final DefaultTableModel productRows = readOnlyModel("Product", "Stock", "Price");
    private final DefaultTableModel historyRows;
    private final JTable productTable = new JTable(productRows);
    private List<Product> products = Collections.emptyList();
    private SQLite sqlite;
    private User displayedUser;

    RoleFeatures(String panelName) {
        super(new BorderLayout());
        this.panelName = panelName;
        customer = "client".equals(panelName);
        canViewHistory = customer || "manager".equals(panelName);
        historyRows = customer
                ? readOnlyModel("Product", "Quantity", "Time")
                : readOnlyModel("User", "Product", "Quantity", "Time");

        JPanel catalog = new JPanel(new BorderLayout());
        catalog.add(new JScrollPane(productTable), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEADING));
        if (customer) {
            addAction(actions, "Purchase", this::purchase);
        } else {
            addAction(actions, "Add", user -> editProduct(user, null));
            addAction(actions, "Edit", user -> {
                Product product = selectedProduct();
                if (product != null) {
                    editProduct(user, product);
                }
            });
            addAction(actions, "Delete", this::deleteProduct);
        }
        addAction(actions, "Refresh", user -> loadProducts());
        catalog.add(actions, BorderLayout.SOUTH);
        tabs.addTab("Products", catalog);
        if (canViewHistory) {
            JPanel history = new JPanel(new BorderLayout());
            history.add(new JScrollPane(new JTable(historyRows)), BorderLayout.CENTER);
            JPanel historyActions = new JPanel(new FlowLayout(FlowLayout.LEADING));
            addAction(historyActions, "Refresh", this::loadHistory);
            history.add(historyActions, BorderLayout.SOUTH);
            tabs.addTab(customer ? "My purchase history" : "Purchase history", history);
        }
        tabs.setVisible(false);
        add(tabs, BorderLayout.CENTER);
        tabs.addChangeListener(event -> refreshForSession());
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    refreshForSession();
                } else {
                    clearDisplay();
                }
            }
        });
    }

    public final void init(SQLite sqlite) {
        this.sqlite = sqlite;
    }

    public final void showPnl(String panel) {
        // Frame calls this before switching cards. Refresh when the card becomes visible.
        if ("home".equals(panel) && isShowing()) {
            refreshForSession();
        }
    }

    private Frame frame() {
        return (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    }

    private User activeUser() {
        Frame frame = frame();
        User user = frame == null ? null : frame.currentUser;
        return AccessControl.canAccess(user, panelName) ? user : null;
    }

    private void clearDisplay() {
        displayedUser = null;
        tabs.setVisible(false);
        products = Collections.emptyList();
        productRows.setRowCount(0);
        historyRows.setRowCount(0);
    }

    private void refreshForSession() {
        try {
            User user = activeUser();
            if (user == null || sqlite == null) {
                clearDisplay();
                return;
            }
            if (displayedUser != user) {
                clearDisplay();
                displayedUser = user;
            }
            if (tabs.getSelectedIndex() == 0) {
                loadProducts();
            } else {
                loadHistory(user);
            }
            tabs.setVisible(true);
        } catch (SQLException | RuntimeException ex) {
            clearDisplay();
            ErrorHandler.show(this);
        }
    }

    private void addAction(JPanel actions, String label, RoleAction action) {
        JButton button = new JButton(label);
        button.addActionListener(event -> {
            try {
                User user = activeUser();
                if (user == null || user != displayedUser) {
                    clearDisplay();
                    ErrorHandler.show(this);
                    return;
                }
                action.run(user);
            } catch (SQLException | RuntimeException ex) {
                ErrorHandler.show(this);
            }
        });
        actions.add(button);
    }

    private void loadProducts() throws SQLException {
        products = Collections.emptyList();
        productRows.setRowCount(0);
        products = sqlite.listProducts();
        for (Product product : products) {
            productRows.addRow(new Object[]{product.getName(), product.getStock(), product.getPriceAsDouble()});
        }
    }

    private void loadHistory(User user) throws SQLException {
        historyRows.setRowCount(0);
        List<History> entries = customer
                ? sqlite.getHistory(user.getUsername()) : sqlite.listPurchaseHistory();
        for (History entry : entries) {
            if (customer) {
                historyRows.addRow(new Object[]{entry.getName(), entry.getStock(), entry.getTimestamp()});
            } else {
                historyRows.addRow(new Object[]{entry.getUsername(), entry.getName(),
                    entry.getStock(), entry.getTimestamp()});
            }
        }
    }

    private Product selectedProduct() {
        int selected = productTable.getSelectedRow();
        if (selected < 0) {
            message("Select a product first.");
            return null;
        }
        return products.get(productTable.convertRowIndexToModel(selected));
    }

    private void editProduct(User user, Product product) throws SQLException {
        JTextField name = new JTextField(product == null ? "" : product.getName(), 24);
        JTextField stock = new JTextField(product == null ? "" : Integer.toString(product.getStock()));
        String displayedPrice = product == null || !Double.isFinite(product.getPriceAsDouble()) ? ""
                : BigDecimal.valueOf(product.getPriceAsDouble()).toPlainString();
        JTextField price = new JTextField(displayedPrice);
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        addField(fields, "Product name", name);
        addField(fields, "Stock", stock);
        addField(fields, "Price", price);
        if (!confirm(fields, product == null ? "Add product" : "Edit product")) {
            return;
        }
        requireSameSession(user);
        int stockValue = parseStock(stock.getText());
        double priceValue = parsePrice(price.getText());
        if (!Validator.isValidProductName(name.getText()) || !Validator.isValidStock(stockValue)
                || !Validator.isValidPrice(priceValue)) {
            reject(user, "Invalid product details",
                    "Enter a valid name (1–50 characters), stock (0–10,000), and price (over 0 to 1,000,000).");
            return;
        }
        if (product == null) {
            sqlite.insertProduct(name.getText(), stockValue, priceValue);
        } else if (!sqlite.updateProduct(product.getId(), product.getName(), product.getStock(),
                name.getText(), stockValue, price.getText().equals(displayedPrice) ? null : priceValue)) {
            message("This product changed. Review the updated list and try again.");
        }
        loadProducts();
    }

    private void deleteProduct(User user) throws SQLException {
        Product product = selectedProduct();
        if (product == null || !confirm("Delete " + product.getName() + "?", "Delete product")) {
            return;
        }
        requireSameSession(user);
        if (!sqlite.deleteProduct(product.getId())) {
            message("This product is no longer available.");
        }
        loadProducts();
    }

    private void purchase(User user) throws SQLException {
        Product product = selectedProduct();
        if (product == null) {
            return;
        }
        JTextField quantity = new JTextField("1", 10);
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        addField(fields, "Quantity", quantity);
        if (!confirm(fields, "Purchase " + product.getName())) {
            return;
        }
        requireSameSession(user);
        int amount = parseStock(quantity.getText());
        if (amount <= 0 || !Validator.isValidStock(amount)) {
            reject(user, "Invalid purchase quantity", "Enter a whole number from 1 to 10,000.");
            return;
        }
        if (!sqlite.purchaseProduct(user.getUsername(), product.getId(), product.getName(), amount)) {
            reject(user, "Product or requested stock unavailable",
                    "The requested quantity is not available. Review the updated list.");
            loadProducts();
            return;
        }
        try {
            frame().main.log("PURCHASE", user.getUsername(), "Product purchased");
        } finally {
            message("Purchase completed.");
        }
        loadProducts();
    }

    private void requireSameSession(User user) {
        if (activeUser() != user || displayedUser != user) {
            clearDisplay();
            throw new IllegalStateException("Session changed");
        }
    }

    private void reject(User user, String reason, String feedback) {
        frame().main.log("VALIDATION_FAILURE", user.getUsername(), reason);
        message(feedback);
    }

    private static int parseStock(String input) {
        return input.length() <= MAX_STOCK_LENGTH && input.matches("[0-9]+")
                ? Integer.parseInt(input) : -1;
    }

    private static double parsePrice(String input) {
        if (input.length() > MAX_PRICE_LENGTH || !input.matches("[0-9]+(\\.[0-9]+)?")) {
            return Double.NaN;
        }
        BigDecimal price = new BigDecimal(input);
        return price.compareTo(BigDecimal.valueOf(1_000_000)) <= 0 ? price.doubleValue() : Double.NaN;
    }

    private static void addField(JPanel fields, String label, JTextField input) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setLabelFor(input);
        fields.add(fieldLabel);
        fields.add(input);
    }

    private boolean confirm(Object contents, String title) {
        return JOptionPane.showConfirmDialog(this, contents, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    @FunctionalInterface
    private interface RoleAction {
        void run(User user) throws SQLException;
    }
}
