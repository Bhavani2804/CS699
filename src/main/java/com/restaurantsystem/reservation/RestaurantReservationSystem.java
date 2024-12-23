package com.restaurantsystem.reservation;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class RestaurantReservationSystem {
    private static final String DB_URL = "jdbc:sqlite:reservations.db";

    private JFrame frame;
    private JTextField customerNameField;
    private JTextField phoneField;
    private JTextField guestCountField;
    private JDateChooser dateChooser;
    private JTextArea specialRequestsArea;
    private JPanel timePickerPanel;
    private JButton selectedButton;
    private JLabel selectedTimeLabel;
    
    private JButton searchButton;
    private JButton updateButton;
    private JButton cancelButton;
    private JButton reservationHistoryButton;
    private JButton removeWaitlistButton;
    private JButton managerLoginButton;
    
    private int currentReservationId = -1;

    public RestaurantReservationSystem() {
        createDatabase();

        frame = new JFrame("ABC Restaurant Reservation");
        frame.setSize(1000, 600);
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

        // Adding the Special Requests field
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 1;
        frame.add(new JLabel("Special Requests:"), gbc);
        gbc.gridx = 1; 
        specialRequestsArea = new JTextArea(3, 20);
        JScrollPane scrollPane = new JScrollPane(specialRequestsArea);
        frame.add(scrollPane, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
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
                String specialRequests = specialRequestsArea.getText();

                if (validateInputs(name, phone, guests, date, timeString)) {
                    saveReservation(name, phone, guests, date, timeString, specialRequests);
                }
            }
        });
     // Search Button for finding existing reservations
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 1;
        searchButton = new JButton("Search Reservation");
        frame.add(searchButton, gbc);

        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchReservation();
            }
        });

        // Update Button for saving changes to the existing reservation
        gbc.gridx = 1; gbc.gridy = 8; gbc.gridwidth = 1;
        updateButton = new JButton("Update Reservation");
        updateButton.setEnabled(false); // Enable only after a successful search
        frame.add(updateButton, gbc);

        updateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateExistingReservation();
            }
        });
        gbc.gridx = 0; gbc.gridy = 11; gbc.gridwidth = 1;
        reservationHistoryButton = new JButton("Reservation History");
        reservationHistoryButton.setEnabled(false);
        frame.add(reservationHistoryButton,gbc);
        
        reservationHistoryButton.addActionListener(e -> viewReservationHistory());
        
        gbc.gridx = 1; gbc.gridy = 11; gbc.gridwidth = 1;
        cancelButton = new JButton("Cancel Reservation");
        cancelButton.setEnabled(false); // Enable only after a successful search
        frame.add(cancelButton, gbc);
        
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelReservation();
            }
        });
        
        gbc.gridx = 0; gbc.gridy = 12; gbc.gridwidth = 1;
        removeWaitlistButton = new JButton("Remove Waitlist");
        removeWaitlistButton.setEnabled(false); // Initially disabled
        removeWaitlistButton.addActionListener(e -> removeFromWaitlist());
        frame.add(removeWaitlistButton, gbc);
        
     // Inside your main UI class
        gbc.gridx = 1;gbc.gridy = 12;gbc.gridwidth = 1;
        managerLoginButton = new JButton("Manager Login");
        managerLoginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openManagerLoginDialog();
            }
        });
        frame.add(managerLoginButton, gbc);
       
        populateTimeOptions();
        setDefaultTimeSlot();
        frame.setVisible(true);
    }
    //Creates a Database
    private void createDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
        	// Assuming we are using SQLite
        	String sql = "CREATE TABLE IF NOT EXISTS reservations (" +
        	             "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        	             "name TEXT, " +
        	             "phoneNumber TEXT, " +
        	             "partySize INTEGER, " +
        	             "date TEXT, " +
        	             "time TEXT, " +
        	             "specialRequests TEXT)";
        	stmt.execute(sql);
        	stmt.executeUpdate(sql);

            sql = "CREATE TABLE IF NOT EXISTS managers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "login_id TEXT NOT NULL UNIQUE," +
                    "password TEXT NOT NULL" +
                    ")";
            stmt.execute(sql);
            sql= "CREATE TABLE IF NOT EXISTS waitlist ("+
            	    "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
            	    "name TEXT NOT NULL,"+
            	    "phone TEXT NOT NULL,"+
            	    "guests INTEGER NOT NULL,"+
            	    "position INTEGER NOT NULL,"+
            	    "added_time DATETIME DEFAULT CURRENT_TIMESTAMP)";
            	stmt.execute(sql);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    
    //Checks if Phone Number is Correct
    private boolean isValidPhoneNumber(String phone) {
        return phone.matches("\\d{3}-\\d{3}-\\d{4}");
    }
    
    //Checks if Guest Count is Valid
    private boolean isValidGuestCount(String guests) {
        try {
            int guestCount = Integer.parseInt(guests);
            return guestCount > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    //Validates the User Input
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
    
    //Saves the reservation into Database
    private void saveReservation(String name, String phone, String guests, Date date, String time, String specialRequests) {
    	String sql = "INSERT INTO reservations (name, phone, reservation_date, reservation_time, guests, specialRequests) VALUES (?, ?, ?, ?, ?, ?)";
    	SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = sdfDate.format(date);

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setString(3, formattedDate);
            pstmt.setString(4, time);
            pstmt.setInt(5, Integer.parseInt(guests));
            pstmt.setString(6, specialRequests);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(frame, "Reservation made successfully!");
            clearForm();
            populateTimeOptions(); // Refresh the available times
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(frame, "Error saving reservation.");
        }
    }
    
    //Shows Warning Dialogs
    private void showMessage(String message) {
        JOptionPane.showMessageDialog(frame, message);
    }
    
    //Shows all the available time slots
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
            	timeButton.setEnabled(true);
            	timeButton.setText("Waitlist");
            	timeButton.setBackground(Color.RED); // Highlight reserved slots in red
            	
            	timeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int response = JOptionPane.showConfirmDialog(frame, 
                            "This time slot is fully booked. Would you like to join the waitlist?", 
                            "Join Waitlist", JOptionPane.YES_NO_OPTION);

                        if (response == JOptionPane.YES_OPTION) {
                        	// Assuming a method to get the number of guests
                            
                            joinWaitlist();
                            JOptionPane.showMessageDialog(frame, "You've been added to the waitlist for " + timeString);
                        }
                    }
                });
            	
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
    
    //Retrieves the booked time slots
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
    
    // Checks if the selected time and date is future or not
    private boolean isFutureDateTime(Date date, String time) {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        
        Calendar selectedDateTime = Calendar.getInstance();
        selectedDateTime.setTime(date);
        selectedDateTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.split(":")[0]));
        selectedDateTime.set(Calendar.MINUTE, Integer.parseInt(time.split(":")[1].substring(0, 2)));
        selectedDateTime.set(Calendar.SECOND, 0);
        selectedDateTime.set(Calendar.MILLISECOND, 0);

        // Check if the selected date is today
        if (selectedDateTime.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            selectedDateTime.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
            // If the date is today, check if the time is in the future
            return selectedDateTime.after(now);
        } else {
            // If the date is in the future, return true
            return selectedDateTime.after(now);
        }
    }
    
    // Sets the default time slot to the next available slot
    private void setDefaultTimeSlot() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");

        String defaultTime = timeFormat.format(now.getTime());
        selectedTimeLabel.setText(defaultTime);
    }
    
    
    //Searching reservation Button
    private void searchReservation() {
        String name = customerNameField.getText();
        String phone = phoneField.getText();

        if (name.isEmpty() || phone.isEmpty()) {
            showMessage("Please enter both name and phone number to search.");
            return;
        }

        String sql = "SELECT * FROM reservations WHERE name = ? AND phone = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
            	    // Retrieve reservation details
            	    currentReservationId = rs.getInt("id");
            	    String reservationDate = rs.getString("reservation_date");
            	    //String reservationTime = rs.getString("reservation_time");
            	    //String specialRequests = rs.getString("specialRequests");

            	    // Parse reservation date and time into a single DateTime object
            	    LocalDate reservationDateTime = LocalDate.parse(reservationDate);

            	    // Get the current date and time
            	    LocalDate currentDateTime = LocalDate.now();

            	    // Check if the reservation is in the past
            	    if (reservationDateTime.isBefore(currentDateTime)) {
            	        showMessage("No upcoming reservations found. You can check your reservation history.");
            	        reservationHistoryButton.setEnabled(true);
            	        rs.close();
            	        return; // Exit the method since it's a past reservation
            	    }
                currentReservationId = rs.getInt("id");
                guestCountField.setText(String.valueOf(rs.getInt("guests")));
                dateChooser.setDate(new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString("reservation_date")));
                specialRequestsArea.setText(rs.getString("specialRequests"));
                selectedTimeLabel.setText(rs.getString("reservation_time"));
                // Enable the update button
                updateButton.setEnabled(true);
                cancelButton.setEnabled(true);
                reservationHistoryButton.setEnabled(true);
                removeWaitlistButton.setEnabled(false);
                showMessage("Reservation found. You can now update the details.");
                rs.close();
            } else {
            	//String phone = phoneField.getText();
            	sql = "SELECT position FROM waitlist WHERE name = ? AND phone = ?";
                try (PreparedStatement waitlistPstmt = conn.prepareStatement(sql)) {
                    waitlistPstmt.setString(1, name);
                    waitlistPstmt.setString(2, phone);
                    ResultSet waitlistRs = waitlistPstmt.executeQuery();

                    if (waitlistRs.next()) {
                        int position = waitlistRs.getInt("position");
                        JOptionPane.showMessageDialog(frame, "Your waitlist position is: " + position);
                        removeWaitlistButton.setEnabled(true);
                    } else {
                        JOptionPane.showMessageDialog(frame, "No Reservation Found");
                    }

                    // Close the waitlist ResultSet
                    waitlistRs.close();}
                
            	//showMessage("No reservation found with the provided name and phone number.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error retrieving reservation.");
        }
    }
    
    // View Reservation history
    private void viewReservationHistory() {
    	String name = customerNameField.getText();
        String phone = phoneField.getText();
        String sql = "SELECT id, name, phone, guests, reservation_date, reservation_time " +
                     "FROM reservations WHERE name = ? AND phone = ? AND datetime(reservation_date) < datetime('now')";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)){
        		pstmt.setString(1, name); // Replace 'customerName' with the actual name variable
        	    pstmt.setString(2, phone);
             try(ResultSet rs = pstmt.executeQuery()) {

            // Table column headers
            String[] columnNames = {"ID", "Name", "Phone", "Guests", "Date", "Time"};
            ArrayList<String[]> data = new ArrayList<>();
            // Fetch data for past reservations only
            while (rs.next()) {
                String[] row = {
                    String.valueOf(rs.getInt("id")),
                    rs.getString("name"),
                    rs.getString("phone"),
                    String.valueOf(rs.getInt("guests")),
                    rs.getString("reservation_date"),
                    rs.getString("reservation_time")
                };
                data.add(row);
            }

            // Convert ArrayList to a 2D array for JTable
            String[][] dataArr = data.toArray(new String[0][0]);

            // Display the data in a JTable within a JScrollPane
            JTable historyTable = new JTable(dataArr, columnNames);
            JScrollPane scrollPane = new JScrollPane(historyTable);

            // Show the table in a dialog
            JOptionPane.showMessageDialog(frame, scrollPane, "Reservation History", JOptionPane.INFORMATION_MESSAGE);

        }} catch (SQLException e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(frame, "Error retrieving reservation history.");
        }
    }

    
    // Modify reservation by Customer
    private void updateExistingReservation() {
        String name = customerNameField.getText();
        String phone = phoneField.getText();
        String guests = guestCountField.getText();
        Date date = dateChooser.getDate();
        String timeString = selectedButton != null ? selectedButton.getText() : selectedTimeLabel.getText();
        String specialRequests = specialRequestsArea.getText();

        if (validateInputs(name, phone, guests, date, timeString)) {
            String sql = "UPDATE reservations SET name = ?, phone = ?, reservation_date = ?, reservation_time = ?, guests = ?, specialRequests = ? WHERE id = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setString(2, phone);
                pstmt.setString(3, new SimpleDateFormat("yyyy-MM-dd").format(date));
                pstmt.setString(4, timeString);
                pstmt.setInt(5, Integer.parseInt(guests));
                pstmt.setString(6, specialRequests);
                pstmt.setInt(7, currentReservationId);

                pstmt.executeUpdate();
                showMessage("Reservation  successfully!");
                clearForm();
            } catch (SQLException e) {
                e.printStackTrace();
                showMessage("Error updating reservation.");
            }
        }
    }
    
    //Clears the reservation form
    private void clearForm() {
        customerNameField.setText("");
        phoneField.setText("");
        guestCountField.setText("");
        dateChooser.setDate(Calendar.getInstance().getTime());
        specialRequestsArea.setText("");
        selectedButton = null;
        selectedTimeLabel.setText("None");
        currentReservationId = -1;
        updateButton.setEnabled(false);
        cancelButton.setEnabled(false);
        reservationHistoryButton.setEnabled(false);
        
    }
   
    
    //Cancel the reservation by customer
    private void cancelReservation() {
    	String name = customerNameField.getText();
        String phone = phoneField.getText();
    	String sql = "DELETE FROM reservations WHERE name = ? AND phone = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(frame, "Cancellation successful!");
            clearForm();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(frame, "Error canceling reservation.");
        }
    }
    
    private void cancelReservation(String phoneNumber) {
        String sql = "DELETE FROM reservations WHERE phone = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phoneNumber);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null, "Reservation cancelled successfully.");
            } else {
                JOptionPane.showMessageDialog(null, "No reservation found with that phone number.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error canceling reservation.");
        }
    }
    
    //Join wait list Management
    
    
    private void joinWaitlist() {
    	String name = customerNameField.getText();
    	String phone = phoneField.getText();
    	String guests = guestCountField.getText();
    	String sql = "INSERT INTO waitlist (name, phone, guests, position) VALUES (?, ?, ?, " +
                     "(SELECT IFNULL(MAX(position), 0) + 1 FROM waitlist))";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setInt(3, Integer.parseInt(guests));
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(frame, "Added to waitlist successfully!");
            //viewWaitlistButton.setEnabled(true);
            removeWaitlistButton.setEnabled(true);
            //resetWaitlistButton();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(frame, "Error adding to waitlist.");
        }
    }
    
    //Remove Wait list
    
    private void removeFromWaitlist() {
    	String phone = phoneField.getText();
        String sql = "DELETE FROM waitlist WHERE phone = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(frame, "Removed from waitlist successfully!");
                //viewWaitlistButton.setEnabled(false);
                removeWaitlistButton.setEnabled(false);
                updateWaitlistPositions(); // Adjust positions after removal
            } else {
                JOptionPane.showMessageDialog(frame, "No entry found with that phone number.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(frame, "Error removing from waitlist.");
        }
    }
    
    private void removeFromWaitlist(String phoneNumber) {
        String sql = "DELETE FROM waitlist WHERE phone = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phoneNumber);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null, "Removed from waitlist successfully.");
            } else {
                JOptionPane.showMessageDialog(null, "No entry found with that phone number.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error removing from waitlist.");
        }
    }
    
    //Update Wait list
    private void updateWaitlistPositions() {
        String sql = "UPDATE waitlist SET position = position - 1 WHERE position > ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    
    //Manager Dialog
    private void openManagerLoginDialog() {
        JDialog loginDialog = new JDialog((Frame) null, "Manager Login", true);
        loginDialog.setSize(300, 200);
        loginDialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel userLabel = new JLabel("Username:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        loginDialog.add(userLabel, gbc);

        JTextField userField = new JTextField(15);
        gbc.gridx = 1;
        gbc.gridy = 0;
        loginDialog.add(userField, gbc);

        JLabel passLabel = new JLabel("Password:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        loginDialog.add(passLabel, gbc);

        JPasswordField passField = new JPasswordField(15);
        gbc.gridx = 1;
        gbc.gridy = 1;
        loginDialog.add(passField, gbc);

        JButton loginButton = new JButton("Login");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        loginDialog.add(loginButton, gbc);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = userField.getText();
                String password = passField.getText();
                if (authenticateManager(username, password)) {
                    loginDialog.dispose();
                    openManagerDashboard();
                } else {
                    JOptionPane.showMessageDialog(loginDialog, "Invalid credentials", "Login Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        loginDialog.setLocationRelativeTo(null);
        loginDialog.setVisible(true);
    }

    private boolean authenticateManager(String username, String password) {
        String sql = "SELECT * FROM managers WHERE login_id = ? AND password = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                // If a match is found, return true for successful authentication
                return true;
            } else {
                // No match found, return false for failed authentication
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error during authentication.");
            return false;
        }
    }

    
    private void openManagerDashboard() {
        JDialog managerDashboard = new JDialog((Frame) null, "Manager Dashboard", true);
        managerDashboard.setSize(300, 100);
        managerDashboard.setLayout(new BorderLayout());

        /*JTextArea infoArea = new JTextArea("Manager functionalities will be displayed here...");
        infoArea.setEditable(false);*/
        //managerDashboard.add(infoArea, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 2, 10, 10));

        // Button for managing the wait list
        JButton manageWaitlistButton = new JButton("Manage Waitlist");
        manageWaitlistButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openWaitlistManagement();
            }
        });
        
        // Button for managing reservations
        JButton manageReservationsButton = new JButton("Manage Reservations");
        manageReservationsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               openReservationManagement();
            }
        });

        // Add buttons to panel
        buttonPanel.add(manageWaitlistButton);
        buttonPanel.add(manageReservationsButton);

        managerDashboard.add(buttonPanel, BorderLayout.SOUTH);

        managerDashboard.setLocationRelativeTo(null);
        managerDashboard.setVisible(true);
    }

    
    private void openWaitlistManagement() {
        JDialog waitlistDialog = new JDialog((Frame) null, "Manage Waitlist", true);
        waitlistDialog.setSize(400, 300);
        waitlistDialog.setLayout(new BorderLayout());

        JTextArea waitlistInfoArea = new JTextArea();
        waitlistInfoArea.setEditable(false);
        /*waitlistInfoArea.setText("Waitlist Information...\n");*/

        // Fetch and display wait list from the database
        String sql = "SELECT * FROM waitlist ORDER BY position";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                String phone = rs.getString("phone");
                int position = rs.getInt("position");
                waitlistInfoArea.append("Position: " + position + ", Name: " + name + ", Phone: " + phone + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(waitlistDialog, "Error retrieving waitlist.");
        }
        waitlistDialog.add(waitlistInfoArea, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton removeFromWaitlistButton = new JButton("Remove from Waitlist");
        removeFromWaitlistButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String phoneNumber = JOptionPane.showInputDialog(waitlistDialog, "Enter phone number to remove from waitlist:");
                removeFromWaitlist(phoneNumber);
            }
        });
        buttonPanel.add(removeFromWaitlistButton);
        waitlistDialog.add(buttonPanel, BorderLayout.SOUTH);

        waitlistDialog.setLocationRelativeTo(null);
        waitlistDialog.setVisible(true);
    }

    private void openReservationManagement() {
        JDialog reservationDialog = new JDialog((Frame) null, "Manage Reservations", true);
        reservationDialog.setSize(400, 300);
        reservationDialog.setLayout(new BorderLayout());

        JTextArea reservationInfoArea = new JTextArea();
        reservationInfoArea.setEditable(false);
        /*reservationInfoArea.setText("Current Reservations...\n");*/

        // Fetch and display reservations from the database
        String sql = "SELECT * FROM reservations ORDER BY reservation_date, reservation_time";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                String phone = rs.getString("phone");
                String date = rs.getString("reservation_date");
                String time = rs.getString("reservation_time");
                reservationInfoArea.append("Name: " + name + ", Phone: " + phone + ", Date: " + date + ", Time: " + time + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(reservationDialog, "Error retrieving reservations.");
        }

        reservationDialog.add(reservationInfoArea, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        JButton cancelReservationButton = new JButton("Cancel Reservation");
        cancelReservationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String phoneNumber = JOptionPane.showInputDialog(reservationDialog, "Enter phone number to cancel reservation:");
                cancelReservation(phoneNumber);
            }
        });
        buttonPanel.add(cancelReservationButton);
        reservationDialog.add(buttonPanel, BorderLayout.SOUTH);

        reservationDialog.setLocationRelativeTo(null);
        reservationDialog.setVisible(true);
    }
    
    
    //Main Method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RestaurantReservationSystem());
    }
}
