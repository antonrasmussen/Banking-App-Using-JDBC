package com.revature.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.revature.model.Account;
import com.revature.model.Customer;
import com.revature.util.ConnectionUtil;

public class BankRepositoryJdbc implements BankRepository {

	private static Logger LOGGER = Logger.getLogger(BankRepositoryJdbc.class);


	// Account methods

	@Override
	public boolean insert(Account account) {
		try(Connection connection = ConnectionUtil.getConnection()){
			int parameterIndex = 0;

			String sql = "INSERT INTO ACCOUNT VALUES (NULL, ?, ?, ?, ?, NULL)";

			PreparedStatement statement = connection.prepareStatement(sql);
			//-->account number is NULL
			statement.setString(++parameterIndex, account.getAccountType());
			statement.setString(++parameterIndex, account.getAccountStatus());
			statement.setDouble(++parameterIndex, account.getAccountBalance());
			statement.setLong(++parameterIndex, account.getCustomer().getId()); // Chain to get PK
			//-->account hash is NULL

			//returns int num of rows
			if(statement.executeUpdate() > 0) {
				return true;
			}
		} catch (SQLException e) {

			LOGGER.error("Couldn't insert account", e);
		}

		return false;
	}
	
	@Override
	public boolean updateBalance(double accountBalance, Long accountNumber) {
		try(Connection connection = ConnectionUtil.getConnection()){
			int parameterIndex = 0;

			String sql = "UPDATE ACCOUNT SET A_ACCOUNT_BALANCE = ? WHERE A_ACCOUNT_NUMBER = ?";

			PreparedStatement statement = connection.prepareStatement(sql);

			statement.setDouble(++parameterIndex, accountBalance);
			statement.setLong(++parameterIndex, accountNumber);

			//returns int num of rows
			if(statement.executeUpdate() > 0) {
				return true;
			}
		} catch (SQLException e) {

			LOGGER.error("Couldn't update balance", e);
		}
		return false;
		
	}


	@Override
	public Set<Account> findAllAccounts() {
		try(Connection connection = ConnectionUtil.getConnection()) {
			String sql = "SELECT * FROM ACCOUNT";
			PreparedStatement statement = connection.prepareStatement(sql);
			ResultSet result = statement.executeQuery();

			//The Account Set
			Set<Account> accounts = new LinkedHashSet<>();
			while(result.next()) {
				accounts.add(new Account(
						result.getLong("A_ACCOUNT_NUMBER"),
						result.getString("A_ACCOUNT_TYPE"),
						result.getString("A_ACCOUNT_STATUS"),
						result.getDouble("A_ACCOUNT_BALANCE"),
						new Customer(
								result.getLong("C_ID")),
						result.getString("ACCOUNT_HASH")
						));
			}

			if(accounts.size() == 0) {

				LOGGER.info("No accounts to display");
				return null;
			}

			return accounts;
		} catch (SQLException e) {

			LOGGER.error("Couldn't retrieve all accounts", e);
		}
		return null;
	}

	@Override
	public Account findByAccountNumber(String loginName, long accountNumber) {
		
		Account account = new Account(accountNumber);
		
		account.setAccountBalance(findSingleBalance(loginName, accountNumber));
		
		return account;
		
	}


	//Customer methods

	@Override
	public boolean insert(Customer customer) {
		LOGGER.info("In bank repository");
		try(Connection connection = ConnectionUtil.getConnection()){
			int parameterIndex = 0;

			String sql = "INSERT INTO CUSTOMER VALUES (NULL, ?, ?, ?, ?, NULL)";

			PreparedStatement statement = connection.prepareStatement(sql);
			//-->customer id is NULL
			statement.setString(++parameterIndex, customer.getFirstName());
			statement.setString(++parameterIndex, customer.getLastName());
			statement.setString(++parameterIndex, customer.getLoginName());
			statement.setString(++parameterIndex, customer.getEmailAddress());
			// Chain to get PK
			//-->customer hash is NULL

			//returns int num of rows
			if(statement.executeUpdate() > 0) {
				LOGGER.info("statement.executeUpdate > 0");
				return true;
			}
		} catch (SQLException e) {

			LOGGER.error("Couldn't insert customer", e);
		}

		return false;
	}


	@Override
	public Set<Customer> findAllCustomers() {
		try(Connection connection = ConnectionUtil.getConnection()) {
			String sql = "SELECT * FROM CUSTOMER";
			PreparedStatement statement = connection.prepareStatement(sql);
			ResultSet result = statement.executeQuery();

			//The Customer Set
			Set<Customer> customers = new LinkedHashSet<>();
			while(result.next()) {
				customers.add(new Customer(
						result.getString("C_FIRST_NAME"),
						result.getString("C_LAST_NAME"),
						result.getString("C_LOGIN_NAME"),
						result.getString("C_EMAIL_ADDRESS")));
			}

			if(customers.size() == 0) {

				LOGGER.info("No customers to display");
				return null;
			}

			return customers;
		} catch (SQLException e) {

			LOGGER.error("Couldn't retrieve all customers", e);
		}
		return null;
	}


	@Override
	public double findBalanceByCustomerId(long id) {
		try(Connection connection = ConnectionUtil.getConnection()) {
			String sql = "SELECT A_ACCOUNT_BALANCE FROM ACCOUNT WHERE C_ID = " + id;
			PreparedStatement statement = connection.prepareStatement(sql);


			ResultSet result = statement.executeQuery();

			//The Customer Set
			double balanceAccumulator = 0;
			while(result.next()) {
				//No need to do SELECT SUM(A_ACCOUNT_BALANCE) because of below accumulator
				balanceAccumulator += result.getDouble("A_ACCOUNT_BALANCE");
			}

			return balanceAccumulator;
		} catch (SQLException e) {

			LOGGER.error("Couldn't retrieve balance", e);
		}
		return 0;
	}

	@Override
	public double findTotalBalance(String loginName) {
		try(Connection connection = ConnectionUtil.getConnection()) {
			//LOGGER.info("In findBalanceByLoginName");

			String sql = "SELECT ACCOUNT.C_ID, "
					+ "ACCOUNT.A_ACCOUNT_NUMBER, "
					+ "ACCOUNT.A_ACCOUNT_BALANCE, "
					+ "CUSTOMER.C_LOGIN_NAME "
					+ "FROM ACCOUNT INNER JOIN CUSTOMER "
					+ "ON ACCOUNT.C_ID = CUSTOMER.C_ID WHERE "
					+ "CUSTOMER.C_LOGIN_NAME = ?";
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, loginName);

			ResultSet result = statement.executeQuery();

			//The Customer Set
			double balanceAccumulator = 0;
			while(result.next()) {
				//No need to do SELECT SUM(A_ACCOUNT_BALANCE) because of below accumulator
				balanceAccumulator += result.getDouble("A_ACCOUNT_BALANCE");
			}

			return balanceAccumulator;
		} catch (SQLException e) {

			LOGGER.error("Couldn't retrieve balance", e);
		}
		return 0;
	}
	
	@Override
	public double findSingleBalance(String loginName, Long accountNumber) {
		try(Connection connection = ConnectionUtil.getConnection()) {
			//LOGGER.info("In findBalanceByLoginName");
			
			int parameterIndex = 0;
			String sql = "SELECT ACCOUNT.C_ID, "
					+ "ACCOUNT.A_ACCOUNT_NUMBER, "
					+ "ACCOUNT.A_ACCOUNT_BALANCE, "
					+ "CUSTOMER.C_LOGIN_NAME "
					+ "FROM ACCOUNT INNER JOIN CUSTOMER "
					+ "ON ACCOUNT.C_ID = CUSTOMER.C_ID WHERE "
					+ "CUSTOMER.C_LOGIN_NAME = ? "
					+ "AND ACCOUNT.A_ACCOUNT_NUMBER = ?";
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, loginName);
			statement.setLong(2, accountNumber);
			ResultSet result = statement.executeQuery();

			//The Customer Set
			double balanceAccumulator = 0;
			while(result.next()) {
				//No need to do SELECT SUM(A_ACCOUNT_BALANCE) because of below accumulator
				balanceAccumulator = result.getDouble("A_ACCOUNT_BALANCE");
			}

			return balanceAccumulator;
		} catch (SQLException e) {

			LOGGER.error("Couldn't retrieve balance", e);
		}
		return 0;
	}
	
	@Override
	public Set<Long> findAccountNumbersByLoginName(String loginName) {
		try(Connection connection = ConnectionUtil.getConnection()) {
			String sql = "SELECT ACCOUNT.A_ACCOUNT_NUMBER "
					+ "FROM ACCOUNT "
					+ "INNER JOIN CUSTOMER "
					+ "ON ACCOUNT.C_ID = CUSTOMER.C_ID "
					+ "WHERE CUSTOMER.C_LOGIN_NAME = ?";
			PreparedStatement statement = connection.prepareStatement(sql);
			
			statement.setString(1, loginName);
			
			ResultSet result = statement.executeQuery();

			//The Customer Set
			Set<Long> accountNumbers = new LinkedHashSet<>();
			while(result.next()) {
				accountNumbers.add(result.getLong("A_ACCOUNT_NUMBER"));
			}

			if(accountNumbers.size() == 0) {

				LOGGER.info("No accounts to display");
				return null;
			}

			return accountNumbers;
		} catch (SQLException e) {

			LOGGER.error("Couldn't retrieve all accounts", e);
		}
		return null;
	}
	
	@Override
	public Set<String> findAccountTypesByLoginName(String loginName) {
		try(Connection connection = ConnectionUtil.getConnection()) {
			String sql = "SELECT ACCOUNT.A_ACCOUNT_TYPE "
					+ "FROM ACCOUNT "
					+ "INNER JOIN CUSTOMER "
					+ "ON ACCOUNT.C_ID = CUSTOMER.C_ID "
					+ "WHERE CUSTOMER.C_LOGIN_NAME = ?";
			PreparedStatement statement = connection.prepareStatement(sql);
			
			statement.setString(1, loginName);
			
			ResultSet result = statement.executeQuery();

			//The Customer Set
			Set<String> accountTypes = new LinkedHashSet<>();
			while(result.next()) {
				accountTypes.add(result.getString("A_ACCOUNT_TYPE"));
			}

			if(accountTypes.size() == 0) {

				LOGGER.info("No accounts to display");
				return null;
			}

			return accountTypes;
		} catch (SQLException e) {

			LOGGER.error("Couldn't retrieve all account types", e);
		}
		return null;
	}
 
	@Override
	public Set<Customer> findByLoginName(String loginName) {
		try(Connection connection = ConnectionUtil.getConnection()) {
			String sql = "SELECT C_LOGIN_NAME FROM CUSTOMER";
			PreparedStatement statement = connection.prepareStatement(sql);
			ResultSet result = statement.executeQuery();

			//The Customer Set
			Set<Customer> customer = new LinkedHashSet<>();
			while(result.next()) {
				customer.add(new Customer(
						result.getString("C_LOGIN_NAME")));
			}

			if(customer.size() == 0) {

				LOGGER.info("NOT a valid customer size");
				return null;
			}

			return customer;
		} catch (SQLException e) {

			LOGGER.error("Couldn't retrieve login names", e);
		}
		return null;
	}

	@Override
	public boolean isValidLoginName(String loginName) {
		
		boolean truthFlag = false;
		Set<Customer> customer = new BankRepositoryJdbc().findByLoginName(loginName);
		for(Customer cust: customer) {

			if(cust.getLoginName().equals(loginName)) {
				return true;
			}
			else {
				truthFlag = false;
			}
		}

		return truthFlag;
	}


	public static void main(String[] args) {
		LOGGER.info(new BankRepositoryJdbc().findAllAccounts());
		LOGGER.info(new BankRepositoryJdbc().findAllCustomers());
		LOGGER.info(new BankRepositoryJdbc().findByLoginName("anton"));
		LOGGER.info(new BankRepositoryJdbc().findBalanceByCustomerId(1L));
		LOGGER.info(new BankRepositoryJdbc().findTotalBalance("anton"));
		LOGGER.info(new BankRepositoryJdbc().findAccountNumbersByLoginName("anton")); // sorted
		LOGGER.info(new BankRepositoryJdbc().findSingleBalance("anton", 123456L));
		//LOGGER.info(new BankRepositoryJdbc().updateBalance(1000.00, 123456L)); //>Updates 


	}

}
