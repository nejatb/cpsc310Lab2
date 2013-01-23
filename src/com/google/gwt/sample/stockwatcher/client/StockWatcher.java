package com.google.gwt.sample.stockwatcher.client;

import java.util.ArrayList;
import java.util.Date;

import javax.persistence.criteria.Root;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Label;
import com.sun.java.swing.plaf.windows.resources.windows;

public class StockWatcher implements EntryPoint{
	private static final int REFRESH_INTERVAL = 7000;//ms
	private VerticalPanel mainPanel = new VerticalPanel();
	private FlexTable stocksFlexTable = new FlexTable();
	private HorizontalPanel addPanel = new HorizontalPanel();
	private TextBox newSymbolTextBox = new TextBox();
	private Button addStockButton = new Button("Add");
	private Label lastUpdatedLabel = new Label();
	private ArrayList<String> stocks = new ArrayList<String>();
	
	private LoginInfo loginInfo = null;
	private VerticalPanel loginPanel = new VerticalPanel();
	private Label loginLabel = new Label("Please sign in to your google acount to access the stockWatcher application.");
	private Anchor signInLink = new Anchor("Sign In");
	private Anchor signOutLink = new Anchor("Sign out");
	private final StockServiceAsync stockService = GWT.create(StockService.class);
	
	/**
	 * Entry point method.
	 */
	public void onModuleLoad(){
		//Check loging status using login service
		LoginServiceAsync loginService = GWT.create(LoginService.class);
		loginService.login(GWT.getHostPageBaseURL(), new AsyncCallback<LoginInfo>() {
			
			@Override
			public void onSuccess(LoginInfo result) {
				// TODO Auto-generated method stub
				loginInfo = result;
				if(loginInfo.isLoggedIn())
					loadStockWatcher();
				else
					loadLogin();
			}
			
			@Override
			public void onFailure(Throwable caught) {
				handleError(caught);
				
			}
		});
	}
	
	private void loadLogin(){
		//Assemble login panel
		signInLink.setHref(loginInfo.getLoginUrl());
		loginPanel.add(loginLabel);
		loginPanel.add(signInLink);
		RootPanel.get("stockList").add(loginPanel);
		
	}
	private void loadStockWatcher() {
		//Set up sign out hyperlink
		signOutLink.setHref(loginInfo.getLogoutUrl());
		
		// create table for stock data
		stocksFlexTable.setText(0, 0, "Symbol");
		stocksFlexTable.setText(0, 1, "Price");
		stocksFlexTable.setText(0, 2, "Change");
		stocksFlexTable.setText(0, 3, "Remove");
		
		// Add styles to elements in the stock list table.
		stocksFlexTable.setCellPadding(6);
		
		//Add styles to elements in the stock list table.
		stocksFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
		stocksFlexTable.addStyleName("watchList");
		stocksFlexTable.getCellFormatter().addStyleName(0, 1, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(0, 2, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(0, 3, "watchListRemoveColumn");
		
		loadStocks();
		
		// assemble add stock panel
		addPanel.add(newSymbolTextBox);
		addPanel.add(addStockButton);
		addPanel.addStyleName("addPanel");
		
		// assemble main panel
		mainPanel.add(signOutLink);
		mainPanel.add(stocksFlexTable);
		mainPanel.add(addPanel);
		mainPanel.add(lastUpdatedLabel);
		
		// associate the main panel with the HTML host page.
		RootPanel.get("stockList").add(mainPanel);
		
		// Move cursor focus to the input box
		newSymbolTextBox.setFocus(true);
		
		//Setup timer to refresh list automatically
		Timer refreshTimer = new Timer(){
			@Override
			public void run(){
				refreshWatchList();
			}
		};
		refreshTimer.scheduleRepeating(REFRESH_INTERVAL);
	
		
		//listen for mouse events on the Add button
		addStockButton.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				addStock();
			}
		});
		
		//listen for keyboard events in the input box
		newSymbolTextBox.addKeyPressHandler(new KeyPressHandler() {
			
			@Override
			public void onKeyPress(KeyPressEvent event) {
				if(event.getCharCode() == KeyCodes.KEY_ENTER){
					addStock();
				}
				
			}
		});
	}
	/**
	 * Add stock to FlexTable. Executed when the user clicks the addStockButton 
	 * or presses enter in the newSymbolTextBox
	 */
	private void addStock(){
		final String symbol = newSymbolTextBox.getText().toUpperCase().trim();
		newSymbolTextBox.setFocus(true);
		
		//Stock code must be between 1 and 10 chars that are numbers,
		//letters, or dots.
		if(!symbol.matches("^[0-9A-Z\\.]{1,10}$")){
			Window.alert("'"+symbol+"'is not a valid symbol.");
			newSymbolTextBox.selectAll();
			return;
		}
		
		newSymbolTextBox.setText("");
		
		// Dont add the stock if it's already in the table
		if(stocks.contains(symbol))
			return;
		addStock(symbol);
	}
	private void displayStock(final String symbol){
		
		// add the stock to the table
		int row = stocksFlexTable.getRowCount();
		stocks.add(symbol);
		stocksFlexTable.setText(row, 0, symbol);
		stocksFlexTable.setWidget(row, 2, new Label());
		stocksFlexTable.getCellFormatter().addStyleName(row, 1, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListNumericColumn");
		stocksFlexTable.getCellFormatter().addStyleName(row, 3, "watchListNumericColumn");
		
		// add a button to remove this stock from the table
		Button removeStockButton = new Button("x");
		removeStockButton.addStyleDependentName("remove");
		removeStockButton.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				removeStock(symbol);
			}
		});
		stocksFlexTable.setWidget(row, 3, removeStockButton);
		
		// get the stock price
		refreshWatchList();
		
		//Add the stock to the table
		
	}
	
	private void handleError(Throwable error){
		Window.alert(error.getMessage());
		if(error instanceof NotLoggedInException){
			Window.Location.replace(loginInfo.getLogoutUrl());
		}
	}
	
	private void removeStock(final String symbol){
		stockService.removeStock(symbol, new AsyncCallback<Void>() {

			@Override
			public void onFailure(Throwable caught) {
				handleError(caught);
				
			}

			@Override
			public void onSuccess(Void result) {
				undisplayStock(symbol);
				
			}
		});
	}
	
	private void undisplayStock(String symbol){
		int removedIndex = stocks.indexOf(symbol);
		stocks.remove(removedIndex);
		stocksFlexTable.removeRow(removedIndex+1);
	}
	
	
	
	/**
	 * Generate random stock prices.
	 */
	private void refreshWatchList() {
		final double MAX_PRICE  = 100.0;
		final double MAX_PRICE_CHANGE = 0.02;
		
		StockPrice[] prices = new StockPrice[stocks.size()];
		for (int i = 0; i<stocks.size(); i++){
			double price = Random.nextDouble() * MAX_PRICE;
			double change = price * MAX_PRICE_CHANGE
					* (Random.nextDouble()* 2.0 - 1.0);
			
			prices[i] = new StockPrice(stocks.get(i),price,change);
		}
		
		updateTable(prices);
		
	}
	
	/**
	 * Update the price and change fields all the rows in the stock table.
	 * @param prices Stock data for all rows
	 */
	private void updateTable(StockPrice[] prices) {
		for ( int i = 0; i<prices.length; i++)
			updateTable(prices[i]);
		
		//Display timestamp showing last refresh.
		lastUpdatedLabel.setText("Last update: " + DateTimeFormat.getMediumDateTimeFormat().format(new Date()));
	}
	
	/**
	 * Update a single row in the stock table.
	 * @param stockPrice Stock data for a single row
	 */
	private void updateTable(StockPrice price) {
		//Make sure the stock is still in the stock table.
		
		if(!stocks.contains(price.getSymbol())){
			return;
		}
		
		int row = stocks.indexOf(price.getSymbol()) + 1;
		
	    // Format the data in the Price and Change fields.
	    String priceText = NumberFormat.getFormat("#,##0.00").format(
	        price.getPrice());
	    NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
	    String changeText = changeFormat.format(price.getChange());
	    String changePercentText = changeFormat.format(price.getChangePercent());

	    // Populate the Price and Change fields with new data.
	    stocksFlexTable.setText(row, 1, priceText);
	   // stocksFlexTable.setText(row, 2, changeText + " (" + changePercentText
	     //   + "%)");
		Label changeWidget = (Label) stocksFlexTable.getWidget(row, 2);
		changeWidget.setText(changeText + " (" + changePercentText + "%)");
		
		//change the color of text in the change field based on its value
		String changeStyleName = "noChange";
		if (price.getChangePercent() <-0.1f){
			changeStyleName = "negativeChange";
			
		}
		else if (price.getChangePercent() > 0.1f){
			changeStyleName = "positiveChange";
		}
		
		changeWidget.setStyleName(changeStyleName);
	
	}
	
	private void loadStocks(){
		stockService.getStocks(new AsyncCallback<String[]>() {

			@Override
			public void onFailure(Throwable caught) {
				handleError(caught);
				
			}

			@Override
			public void onSuccess(String[] symbols) {
				displayStocks(symbols);
				
			}
		});
	}
	
	private void displayStocks(String[] symbols){
		for(String symbol : symbols){
			displayStock(symbol);
		}
	}
	
	private void addStock(final String symbol){
		stockService.addStock(symbol, new AsyncCallback<Void>() {

			@Override
			public void onFailure(Throwable caught) {
				handleError(caught);
				
			}

			@Override
			public void onSuccess(Void result) {
				displayStock(symbol);
				
			}
		});
	}
}