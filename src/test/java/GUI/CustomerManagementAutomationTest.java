package GUI;

import CODE.DBConnection;
import java.awt.Component;
import java.awt.Container;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTextField;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.data.TableCell;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automated GUI tests for the Customer Management module (JUnit 5 + AssertJ Swing).
 *
 * AT-01  Window loads the customer list          (manual TC-CM-001)
 * AT-02  Add a customer with valid data          (manual TC-CM-004)
 * AT-03  Add a customer without a name           (manual TC-CM-005)
 * AT-04  Empty phone number is rejected          (manual TC-CM-006)
 * AT-05  Phone with 9 digits is rejected         (manual TC-CM-007)
 * AT-06  Phone with letters is rejected          (manual TC-CM-008)
 * AT-07  Duplicate phone number is rejected      (manual TC-CM-009)
 * AT-08  Search by full phone number             (manual TC-CM-010)
 * AT-09  Search by part of a name                (manual TC-CM-011)
 * AT-10  Empty search shows all customers again  (manual TC-CM-012)
 * AT-11  Search with no match                    (manual TC-CM-013)
 * AT-12  Delete a customer and confirm Yes       (manual TC-CM-017)
 *
 * Every test starts with one test customer (phone 0700000002) in the database.
 * After every test, all customers whose phone starts with 070000 are deleted,
 * so the real data is never changed.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class CustomerManagementAutomationTest {

    private static final String TEST_NAME = "Autotest Kamal";
    private static final String TEST_PHONE = "0700000002";

    private static final String MSG_SAVED = "Customer saved successfully.";
    private static final String MSG_EMPTY_PHONE = "Phone number cannot be empty.";
    private static final String MSG_BAD_PHONE = "Enter a valid 10-digit phone number starting with 0.";
    private static final String MSG_DUPLICATE = "A customer with this phone number already exists.";
    private static final String MSG_DELETED = "Customer deleted successfully.";

    private Robot robot;
    private FrameFixture window;

    // ------------------------------------------------------------------
    // Set-up and clean-up
    // ------------------------------------------------------------------
    @BeforeEach
    void setUp() {
        cleanTestData();
        insertTestCustomer();

        robot = BasicRobot.robotWithNewAwtHierarchy();
        robot.settings().delayBetweenEvents(200);      // slower, so the video is easy to follow

        Customer_management frame = GuiActionRunner.execute(() -> new Customer_management());
        window = new FrameFixture(robot, frame);
        window.show();
    }

    @AfterEach
    void tearDown() {
        if (window != null) {
            window.cleanUp();      // closes the windows and releases the robot
        }
        cleanTestData();
    }

    // ------------------------------------------------------------------
    // The 12 automated tests
    // ------------------------------------------------------------------
    @Test
    @DisplayName("AT-01 Window loads the customer list")
    void at01_windowLoadsCustomerList() {
        assertThat(window.table().target().getColumnName(0)).isEqualTo("NAME");
        assertThat(window.table().target().getColumnName(1)).isEqualTo("PHONE");
        assertThat(window.table().target().getColumnName(2)).isEqualTo("LOYALTY");
        assertThat(window.table().target().getColumnName(3)).isEqualTo("POINTS");
        assertThat(window.table().target().getColumnCount()).isEqualTo(5);

        assertThat(window.table().rowCount()).isEqualTo(countAllCustomers());
        assertThat(rowOfPhone(TEST_PHONE)).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("AT-02 Add a customer with valid data")
    void at02_addCustomerWithValidData() {
        fillAndSave("Auto Tester", "0700000001");
        clickOkOnMessage(MSG_SAVED);

        int row = rowOfPhone("0700000001");
        assertThat(row).isGreaterThanOrEqualTo(0);
        String[] savedRow = window.table().contents()[row];
        assertThat(savedRow[0]).isEqualTo("Auto Tester");
        assertThat(savedRow[3]).isEqualTo("0");               // new customers start with 0 points
        assertThat(countByPhone("0700000001")).isEqualTo(1);  // and it is really in the database
    }

    @Test
    @DisplayName("AT-03 Add a customer without a name")
    void at03_addCustomerWithoutName() {
        fillAndSave("", "0700000003");
        clickOkOnMessage(MSG_SAVED);

        int row = rowOfPhone("0700000003");
        assertThat(row).isGreaterThanOrEqualTo(0);
        assertThat(window.table().contents()[row][0]).isEqualTo("-");   // no name is shown as "-"
        assertThat(countByPhone("0700000003")).isEqualTo(1);
    }

    @Test
    @DisplayName("AT-04 Empty phone number is rejected")
    void at04_emptyPhoneNumberIsRejected() {
        int before = countAllCustomers();

        DialogFixture dialog = fillAndSave("Empty Phone", "");
        clickOkOnMessage(MSG_EMPTY_PHONE);
        dialog.close();

        assertThat(countAllCustomers()).isEqualTo(before);    // nothing was saved
    }

    @Test
    @DisplayName("AT-05 Phone with 9 digits is rejected")
    void at05_phoneWithNineDigitsIsRejected() {
        int before = countAllCustomers();

        DialogFixture dialog = fillAndSave("Short Phone", "070000000");
        clickOkOnMessage(MSG_BAD_PHONE);
        dialog.close();

        assertThat(countAllCustomers()).isEqualTo(before);    // nothing was saved
    }

    @Test
    @DisplayName("AT-06 Phone with letters is rejected")
    void at06_phoneWithLettersIsRejected() {
        int before = countAllCustomers();

        DialogFixture dialog = fillAndSave("Letter Phone", "070abc0000");
        clickOkOnMessage(MSG_BAD_PHONE);
        dialog.close();

        assertThat(countAllCustomers()).isEqualTo(before);    // nothing was saved
    }

    @Test
    @DisplayName("AT-07 Duplicate phone number is rejected")
    void at07_duplicatePhoneNumberIsRejected() {
        int before = countAllCustomers();

        DialogFixture dialog = fillAndSave("Duplicate Person", TEST_PHONE);
        clickOkOnMessage(MSG_DUPLICATE);
        dialog.close();

        assertThat(countAllCustomers()).isEqualTo(before);    // nothing was saved
        assertThat(countByPhone(TEST_PHONE)).isEqualTo(1);    // still only one customer
    }

    @Test
    @DisplayName("AT-08 Search by full phone number")
    void at08_searchByFullPhoneNumber() {
        searchFor(TEST_PHONE);

        assertThat(window.table().rowCount()).isEqualTo(1);
        String[] row = window.table().contents()[0];
        assertThat(row[0]).isEqualTo(TEST_NAME);
        assertThat(row[1]).isEqualTo(TEST_PHONE);
    }

    @Test
    @DisplayName("AT-09 Search by part of a name")
    void at09_searchByPartOfName() {
        searchFor("autotest");                                // lower case on purpose

        String[][] rows = window.table().contents();
        assertThat(rows.length).isGreaterThanOrEqualTo(1);
        for (String[] row : rows) {
            assertThat(row[0].toLowerCase()).contains("autotest");
        }
        assertThat(rowOfPhone(TEST_PHONE)).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("AT-10 Empty search shows all customers again")
    void at10_emptySearchShowsAllCustomers() {
        searchFor(TEST_PHONE);
        assertThat(window.table().rowCount()).isEqualTo(1);

        window.textBox().deleteText();                        // clear the search box
        findButton(window.target(), "Search").click();

        assertThat(window.table().rowCount()).isEqualTo(countAllCustomers());
    }

    @Test
    @DisplayName("AT-11 Search with no match")
    void at11_searchWithNoMatch() {
        searchFor("zzzzz");

        assertThat(window.table().rowCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("AT-12 Delete a customer and confirm Yes")
    void at12_deleteCustomerAndConfirmYes() {
        searchFor(TEST_PHONE);
        assertThat(window.table().rowCount()).isEqualTo(1);

        window.table().cell(TableCell.row(0).column(4)).click();      // the red cross

        JOptionPaneFixture confirm = JOptionPaneFinder.findOptionPane().using(robot);
        assertThat(String.valueOf(confirm.target().getMessage())).contains("Delete this customer?");
        confirm.yesButton().click();

        clickOkOnMessage(MSG_DELETED);
        assertThat(countByPhone(TEST_PHONE)).isEqualTo(0);
        assertThat(window.table().rowCount()).isEqualTo(0);
    }

    // ------------------------------------------------------------------
    // GUI helper methods
    // ------------------------------------------------------------------

    /** Types the text in the search box and clicks the Search button. */
    private void searchFor(String text) {
        window.textBox().enterText(text);
        findButton(window.target(), "Search").click();
    }

    /** Finds a visible button whose text contains the given text. */
    private JButtonFixture findButton(Container container, String text) {
        JButton button = robot.finder().find(container, new GenericTypeMatcher<JButton>(JButton.class) {
            @Override
            protected boolean isMatching(JButton b) {
                return b.isShowing() && b.getText() != null && b.getText().contains(text);
            }
        });
        return new JButtonFixture(robot, button);
    }

    /** Opens the New Customer dialog, types the name and phone (if not empty), clicks Save Customer. */
    private DialogFixture fillAndSave(String name, String phone) {
        findButton(window.target(), "New Customer").click();
        DialogFixture dialog = WindowFinder.findDialog(New_customer.class).withTimeout(5000).using(robot);

        // The dialog has two text boxes: the upper one is the full name, the lower one is the phone.
        Collection<? extends Component> found = robot.finder().findAll(dialog.target(),
                new GenericTypeMatcher<JTextField>(JTextField.class) {
            @Override
            protected boolean isMatching(JTextField field) {
                return field.isShowing();
            }
        });
        List<JTextField> fields = new ArrayList<>();
        for (Component c : found) {
            fields.add((JTextField) c);
        }
        fields.sort((a, b) -> Integer.compare(
                GuiActionRunner.execute(() -> a.getY()),
                GuiActionRunner.execute(() -> b.getY())));

        if (!name.isEmpty()) {
            new JTextComponentFixture(robot, fields.get(0)).enterText(name);
        }
        if (!phone.isEmpty()) {
            new JTextComponentFixture(robot, fields.get(1)).enterText(phone);
        }
        findButton(dialog.target(), "Save Customer").click();
        return dialog;
    }

    /** Checks the text of the message box that is showing, then clicks OK. */
    private void clickOkOnMessage(String expectedMessage) {
        JOptionPaneFixture pane = JOptionPaneFinder.findOptionPane().using(robot);
        assertThat(String.valueOf(pane.target().getMessage())).contains(expectedMessage);
        pane.okButton().click();
    }

    /** Returns the table row that shows the given phone number, or -1. */
    private int rowOfPhone(String phone) {
        String[][] rows = window.table().contents();
        for (int i = 0; i < rows.length; i++) {
            if (phone.equals(rows[i][1])) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Database helper methods (they use the project's own DBConnection class)
    // ------------------------------------------------------------------
    private static void insertTestCustomer() {
        try (Connection conn = DBConnection.getConnection()) {
            String tier = "Bronze";
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT tier_name FROM loyalty_tiers ORDER BY min_points ASC LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    tier = rs.getString(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO customers (phone_number, full_name, loyalty_tier, loyalty_points) VALUES (?, ?, ?, 0)")) {
                ps.setString(1, TEST_PHONE);
                ps.setString(2, TEST_NAME);
                ps.setString(3, tier);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not add the test customer", e);
        }
    }

    private static void cleanTestData() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM customers WHERE phone_number LIKE '070000%'")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not remove the test customers", e);
        }
    }

    private static int countAllCustomers() {
        return queryCount("SELECT COUNT(*) FROM customers", null);
    }

    private static int countByPhone(String phone) {
        return queryCount("SELECT COUNT(*) FROM customers WHERE phone_number = ?", phone);
    }

    private static int queryCount(String sql, String parameter) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (parameter != null) {
                ps.setString(1, parameter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
