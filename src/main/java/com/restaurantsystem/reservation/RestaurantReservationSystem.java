package com.restaurantsystem.reservation;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class RestaurantReservationSystem {
    private static final String DB_URL = "jdbc:sqlite:reservations.db";

    private JFrame frame;
    private JTextField customerNameField;
    private JTextField phoneField;
    private JTextField guestCountField;
    private JDateChooser dateChooser;
    private JPanel timePickerPanel;
    private JButton selectedButton;
    private JLabel selectedTimeLabel;

    public RestaurantReservationSystem() {
        createDatabase();

        frame = new JFrame("ABC Restaurant Reservation");
        frame.setSize(1000, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.5;
        frame.add(new JLabel("Customer Name:"), gbc);
        gbc.gridx = 1; frame.add(customerNameField = new JTextField(), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        frame.add(new JLabel("Phone (e.g., 123-456-7890):"), gbc);
        gbc.gridx = 1; frame.add(phoneField = new JTextField(), gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        frame.add(new JLabel("Number of Guests:"), gbc);
        gbc.gridx = 1; frame.add(guestCountField = new JTextField(), gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        frame.add(new JLabel("Reservation Date:"), gbc);
        gbc.gridx = 1; frame.add(dateChooser = new JDateChooser(), gbc);

        Calendar calendar = Calendar.getInstance();
        dateChooser.setMinSelectableDate(calendar.getTime());
        dateChooser.setDate(calendar.getTime());

        dateChooser.addPropertyChangeListener("date", evt -> {
            populateTimeOptions();
            setDefaultTimeSlot();
        });

        gbc.gridx = 0; gbc.gridy = 4;
        frame.add(new JLabel("Reservation Time:"), gbc);
        gbc.gridx = 1; frame.add(selectedTimeLabel = new JLabel("None"), gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        timePickerPanel = new JPanel();
        timePickerPanel.setLayout(new GridLayout(3, 4, 10, 10));
        frame.add(timePickerPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        JButton submitButton = new JButton("Reserve");
        submitButton.setBackground(Color.BLUE);
        submitButton.setForeground(Color.BLUE);
        frame.add(submitButton, gbc);

        submitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String name = customerNameField.getText();
                String phone = phoneField.getText();
                String guests = guestCountField.getText();
                Date date = dateChooser.getDate();
                String timeString = selectedButton != null ? selectedButton.getText() : null;

                if (validateInputs(name, phone, guests, date, timeString)) {
                    saveReservation(name, phone, guests, date, timeString);
                }
            }
        });

        // Button for manager login
        JButton managerButton = new JButton("Manager Login");
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        frame.add(managerButton, gbc);
        managerButton.addActionListener(e -> showManagerLogin());

        populateTimeOptions();
        setDefaultTimeSlot();
        frame.setVisible(true);
    }

    private void createDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS reservations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "customer_name TEXT NOT NULL," +
                    "phone TEXT NOT NULL," +
                    "reservation_date TEXT NOT NULL," +
                    "reservation_time TEXT NOT NULL," +
                    "guest_count INTEGER NOT NULL" +
                    ")";
            stmt.execute(sql);

            // Create manager table
            sql = "CREATE TABLE IF NOT EXISTS managers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "login_id TEXT NOT NULL UNIQUE," +
                    "password TEXT NOT NULL" +
                    ")";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean validateInputs(String name, String phone, String guests, Date date, String time) {
        if (name.isEmpty()) {
            showMessage("Customer name cannot be empty.");
            return false;
        }

        if (!isValidPhoneNumber(phone)) {
            showMessage("Please enter a valid phone number (e.g., 123-456-7890).");
            return false;
        }

        if (!isValidGuestCount(guests)) {
            showMessage("Please enter a valid guest count.");
            return false;
        }

        if (date == null) {
            showMessage("Please select a reservation date.");
            return false;
        }

        if (time == null) {
            showMessage("Please select a reservation time.");
            return false;
        }

        if (!isFutureDateTime(date, time)) {
            showMessage("Please select a future date and time.");
            return false;
        }

        return true;
    }

    private boolean isValidPhoneNumber(String phone) {
        return phone.matches("\\d{3}-\\d{3}-\\d{4}");
    }

    private boolean isValidGuestCount(String guests) {
        try {
            int guestCount = Integer.parseInt(guests);
            return guestCount > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void saveReservation(String name, String phone, String guests, Date date, String time) {
        String sql = "INSERT INTO reservations(customer_name, phone, reservation_date, reservation_time, guest_count) VALUES(?, ?, ?, ?, ?)";
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = sdfDate.format(date);

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setString(3, formattedDate);
            pstmt.setString(4, time);
            pstmt.setInt(5, Integer.parseInt(guests));
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(frame, "Reservation made successfully!");
            populateTimeOptions(); // Refresh the available times
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(frame, "Error saving reservation.");
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(frame, message);
    }

    private void populateTimeOptions() {
        timePickerPanel.removeAll();

        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        Calendar selectedDate = Calendar.getInstance();
        if (dateChooser.getDate() != null) {
            selectedDate.setTime(dateChooser.getDate());
        }

        boolean isToday = selectedDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                          selectedDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);

        Calendar slotTime = Calendar.getInstance();
        slotTime.set(Calendar.HOUR_OF_DAY, 11);
        slotTime.set(Calendar.MINUTE, 30);
        slotTime.set(Calendar.SECOND, 0);
        slotTime.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");

        // Fetch booked slots
        String bookedTimes = getBookedTimes(selectedDate);

        while (slotTime.get(Calendar.HOUR_OF_DAY) < 20 || 
               (slotTime.get(Calendar.HOUR_OF_DAY) == 20 && slotTime.get(Calendar.MINUTE) <= 30)) {

            String timeString = timeFormat.format(slotTime.getTime());
            JButton timeButton = new JButton(timeString);
            timeButton.setPreferredSize(new Dimension(100, 30));

            // Check if the time is booked
            if (bookedTimes.contains(timeString)) {
                timeButton.setEnabled(false);
                timeButton.setText("Reserved");
                timeButton.setBackground(Color.RED); // Highlight reserved slots in red
            } else if (isToday && slotTime.before(now)) {
                // If it's today and the slot is in the past, disable and gray out the button
                timeButton.setEnabled(false);
                timeButton.setBackground(Color.GRAY); // Gray out past times
            } else {
                // Available time slots
                timeButton.setEnabled(true);
                timeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (selectedButton != null) {
                            selectedButton.setBackground(null);
                        }
                        timeButton.setBackground(Color.ORANGE);
                        selectedButton = timeButton;
                        selectedTimeLabel.setText(timeString);
                    }
                });
            }

            timePickerPanel.add(timeButton);
            slotTime.add(Calendar.MINUTE, 30);
        }

        timePickerPanel.revalidate();
        timePickerPanel.repaint();
    }

    private String getBookedTimes(Calendar date) {
        StringBuilder bookedTimes = new StringBuilder();
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");

        String sql = "SELECT reservation_time FROM reservations WHERE reservation_date = ?";
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = sdfDate.format(date.getTime());

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, formattedDate);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                bookedTimes.append(rs.getString("reservation_time")).append(",");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return bookedTimes.toString();
    }

    private boolean isFutureDateTime(Date date, String time) {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        
        Calendar selectedDateTime = Calendar.getInstance();
        selectedDateTime.setTime(date);
        selectedDateTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.split(":")[0]));
        selectedDateTime.set(Calendar.MINUTE, Integer.parseInt(time.split(":")[1].substring(0, 2)));
        
        return selectedDateTime.after(now);
    }

    private void setDefaultTimeSlot() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");

        String defaultTime = timeFormat.format(now.getTime());
        selectedTimeLabel.setText(defaultTime);
    }

    private void showManagerLogin() {
        JPanel panel = new JPanel(new GridLayout(3, 2));
        JTextField loginField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        panel.add(new JLabel("Login ID:"));
        panel.add(loginField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Manager Login", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String loginId = loginField.getText();
            String password = new String(passwordField.getPassword());
            if (authenticateManager(loginId, password)) {
                showManagerOptions();
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid login credentials.");
            }
        }
    }

    private boolean authenticateManager(String loginId, String password) {
        String sql = "SELECT * FROM managers WHERE login_id = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, loginId);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // If a result exists, the manager is authenticated
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private void showManagerOptions() {
        JDialog managerDialog = new JDialog(frame, "Manager Options", true);
        managerDialog.setSize(400, 300);
        managerDialog.setLayout(new GridLayout(0, 1));

        JButton viewButton = new JButton("View Reservations");
        viewButton.addActionListener(e -> viewReservations());
        managerDialog.add(viewButton);

        managerDialog.setVisible(true);
    }

    private void viewReservations() {
        JDialog reservationsDialog = new JDialog(frame, "Reservations", true);
        reservationsDialog.setSize(500, 400);
        reservationsDialog.setLayout(new GridLayout(0, 1));

        String sql = "SELECT * FROM reservations";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int reservationId = rs.getInt("id");
                String name = rs.getString("customer_name");
                String phone = rs.getString("phone");
                String date = rs.getString("reservation_date");
                String time = rs.getString("reservation_time");
                int guests = rs.getInt("guest_count");

                JPanel reservationPanel = new JPanel(new FlowLayout());
                reservationPanel.add(new JLabel("ID: " + reservationId + ", Name: " + name +
                        ", Phone: " + phone +
                        ", Date: " + date +
                        ", Time: " + time +
                        ", Guests: " + guests));
                JButton editButton = new JButton("Edit");
                editButton.addActionListener(e -> {
                    modifyReservation(reservationId);
                    reservationsDialog.dispose();
                });
                reservationPanel.add(editButton);
                reservationsDialog.add(reservationPanel);
            }
            reservationsDialog.setVisible(true);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(frame, "Error fetching reservations.");
        }
    }

    private void modifyReservation(int reservationId) {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reservationId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String existingName = rs.getString("customer_name");
                String existingPhone = rs.getString("phone");
                int existingGuests = rs.getInt("guest_count");
                String existingDate = rs.getString("reservation_date");
                String existingTime = rs.getString("reservation_time");

                JPanel panel = new JPanel(new GridLayout(5, 2));
                JTextField nameField = new JTextField(existingName);
                JTextField phoneField = new JTextField(existingPhone);
                JTextField guestCountField = new JTextField(String.valueOf(existingGuests));
                JTextField dateField = new JTextField(existingDate);
                JTextField timeField = new JTextField(existingTime);

                panel.add(new JLabel("Customer Name:"));
                panel.add(nameField);
                panel.add(new JLabel("Phone:"));
                panel.add(phoneField);
                panel.add(new JLabel("Number of Guests:"));
                panel.add(guestCountField);
                panel.add(new JLabel("Reservation Date (yyyy-MM-dd):"));
                panel.add(dateField);
                panel.add(new JLabel("Reservation Time:"));
                panel.add(timeField);

                int result = JOptionPane.showConfirmDialog(frame, panel, "Edit Reservation", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    updateReservation(reservationId, nameField.getText(), phoneField.getText(),
                            guestCountField.getText(), dateField.getText(), timeField.getText());
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(frame, "Error fetching reservation details.");
        }
    }

    private void updateReservation(int reservationId, String name, String phone, String guests, String date, String time) {
        String sql = "UPDATE reservations SET customer_name = ?, phone = ?, guest_count = ?, reservation_date = ?, reservation_time = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setInt(3, Integer.parseInt(guests));
            pstmt.setString(4, date);
            pstmt.setString(5, time);
            pstmt.setInt(6, reservationId);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(frame, "Reservation updated successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(frame, "Error updating reservation.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RestaurantReservationSystem());
    }
}
