package dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import model.Administrator;
import model.Customer;
import model.Gender;
import model.Manifestation;
import model.Seller;
import model.Ticket;
import model.TicketState;
import model.TypeOfCustomer;
import model.TypesOfCustomers;
import model.User;

public class UserDAO {
	private Map<String, User> users = new HashMap<>();

	public UserDAO() {
	}

	public UserDAO(String contextPath, TicketDAO dao, ManifestationDAO manDao) {
		loadData(contextPath, dao, manDao);
	}

	public User addUser(User user) {
		users.put(user.getUsername(), user);
		return user;
	}

	public boolean checkTicket(String manifestationName, Customer customer) {
		for (Ticket t : customer.getTickets()) {
			if (t.getReservedManifestation().getName().equals(manifestationName)
					&& t.getState().equals(TicketState.RESERVED)) {
				return false;
			}
		}
		return true;
	}

	public User find(String username) {
		return users.containsKey(username) ? users.get(username) : null;
	}

	public Collection<User> findAll() {
		return users.values();
	}
	
	public User updateUser(String username, User user) {
		User updatingUser = users.get(username);
		users.remove(username);
		updatingUser.setUsername(user.getUsername());
		updatingUser.setPassword(user.getPassword());
		updatingUser.setName(user.getName());
		updatingUser.setLastName(user.getLastName());
		updatingUser.setDateOfBirth(user.getDateOfBirth());
		users.put(updatingUser.getUsername(), updatingUser);
		
		return updatingUser;
	}

	private void loadData(String contextPath, TicketDAO dao, ManifestationDAO manDao) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy.");
		BufferedReader in = null;
		try {
			String separator = System.getProperty("file.separator");
			File file = new File(contextPath + "data" + separator + "users.txt");
			in = new BufferedReader(new FileReader(file));
			String line;
			StringTokenizer st;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.equals("") || line.indexOf('#') == 0)
					continue;
				st = new StringTokenizer(line, ";");
				while (st.hasMoreTokens()) {
					String username = st.nextToken().trim();
					String password = st.nextToken().trim();
					String name = st.nextToken().trim();
					String lastName = st.nextToken().trim();
					Gender gender = Gender.valueOf(st.nextToken().trim());
					Date date = sdf.parse(st.nextToken().trim());
					// Razvrstavanje korisnika
					String typeOfUser = st.nextToken().trim();

					if (typeOfUser.equals("ADMIN")) {
						boolean deleteda = Boolean.parseBoolean(st.nextToken().trim());
						users.put(username,
								new Administrator(username, password, name, lastName, gender, date, deleteda));
						continue;
					}
					if (typeOfUser.equals("SELLER")) {
						String manifestations = st.nextToken().trim();
						boolean blocked = Boolean.parseBoolean(st.nextToken().trim());
						boolean deleted = Boolean.parseBoolean(st.nextToken().trim());
						List<Manifestation> sellerManifestations = new ArrayList<Manifestation>();
						if (!manifestations.equals("NO_MANIFESTATIONS") && !manifestations.contains(",")) {
							sellerManifestations.add(manDao.find(manifestations));
						} else if (!manifestations.equals("NO_MANIFESTATIONS") && manifestations.contains(",")) {
							String[] params = manifestations.split(",");
							for (String param : params) {
								sellerManifestations.add(manDao.find(param));
							}
						}
						Seller seller = new Seller(username, password, name, lastName, gender, date, blocked, deleted);
						seller.setManifestations(sellerManifestations);
						users.put(username, seller);
						continue;
					}
					if (typeOfUser.equals("CUSTOMER")) {
						String tickets = st.nextToken().trim();
						List<Ticket> customerTickets = new ArrayList<Ticket>();
						if (!tickets.equals("NO_TICKET") && !tickets.contains(",")) {
							customerTickets.add(dao.find(tickets));
						} else if (!tickets.equals("NO_TICKET") && tickets.contains(",")) {
							String[] params = tickets.split(",");
							for (String param : params) {
								customerTickets.add(dao.find(param));
							}
						}
						Double collectedPoints = Double.parseDouble(st.nextToken().trim());
						TypesOfCustomers type = TypesOfCustomers.valueOf(st.nextToken().trim());
						boolean blocked = Boolean.parseBoolean(st.nextToken().trim());
						boolean deleted = Boolean.parseBoolean(st.nextToken().trim());
						TypeOfCustomer customerType = new TypeOfCustomer(type);
						Customer customer = new Customer(username, password, name, lastName, gender, date,
								collectedPoints, customerType, blocked, deleted);
						customer.setTickets(customerTickets);
						users.put(username, customer);
						continue;
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public void saveData(String contextPath) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy.");
		StringBuilder builder = new StringBuilder();
		for (User user : users.values()) {
			builder.append(user.getUsername() + ";");
			builder.append(user.getPassword() + ";");
			builder.append(user.getName() + ";");
			builder.append(user.getLastName() + ";");
			builder.append(user.getGender() + ";");
			builder.append(sdf.format(user.getDateOfBirth()) + ";");
			if (user instanceof Administrator) {
				builder.append("ADMIN;");
			} else if (user instanceof Seller) {
				builder.append("SELLER;");
				Seller seller = (Seller) user;
				if (seller.getManifestations().size() == 0) {
					builder.append("NO_MANIFESTATIONS;");
				} else {
					int counter = 1;
					for (Manifestation manifestation : seller.getManifestations()) {
						builder.append(manifestation.getName());
						if (seller.getManifestations().size() != counter++) {
							builder.append(",");
						}
					}
				}
				builder.append(";");
				builder.append(((Seller) user).isBlocked() + ";");

			} else {
				builder.append("CUSTOMER;");
				Customer customer = (Customer) user;
				if (customer.getTickets().size() == 0) {
					builder.append("NO_TICKET;");
				} else {
					int counter = 1;
					for (Ticket ticket : customer.getTickets()) {
						builder.append(ticket.getId());
						if (customer.getTickets().size() == counter++) {
							builder.append(";");
						} else {
							builder.append(",");
						}
					}
				}
				builder.append(customer.getCollectedPoints() + ";");
				builder.append(customer.getType().getName() + ";");
				builder.append(((Customer) user).isBlocked() + ";");
			}
			builder.append(user.isDeleted());
			builder.append("\n");
		}
		try {
			String separator = System.getProperty("file.separator");
			File file = new File(contextPath + "data" + separator + "users.txt");
			PrintWriter myWriter = new PrintWriter(file);
			myWriter.write(builder.toString());
			myWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
