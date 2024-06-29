package com.demo.zerodhatax;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Main {

    final String Q1 = "Q1";
    final String Q2 = "Q2";
    final String Q3 = "Q3";
    final String Q4 = "Q4";
    final String Q5 = "Q5";
    Float EQ_LONG_TERM = 365.0f;
    Float DEBT_LONG_TERM = 1095.0f;

    Map<Integer, Float> epfInterest = Map.of(2024, 8.25f, 2023, 8.15f, 2022, 8.10f);

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public CommandLineRunner start() {
        return (args) -> {
            Scanner scanner = new Scanner(System.in);
            System.out.print("1. Zerodha Dividend Summary\n");
            System.out.print("2. Zerodha P&L Summary\n");
            System.out.print("3. EPFO Tax\n");
            String inputType = scanner.nextLine();
            switch (inputType) {
                case "1":
                    System.out.println("Zerodha dividend file path");
                    String dividendFile = scanner.nextLine();
                    processDividend(dividendFile);
                    break;
                case "2":
                    System.out.println("Zerodha tax p&l file path:");
                    String taxpnlFile = scanner.nextLine();
                    System.out.println();
                    List<String> equityMfList = initEquityList(taxpnlFile);
                    processEquity(taxpnlFile, equityMfList);
                    processDebt(taxpnlFile, equityMfList);
                    break;
                case "3":
                    processEpfoTax();
                    break;
            }
        };
    }

    @SneakyThrows
    private List<String> initEquityList(String fileName) {
        List<String> equityMfList = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(new File(fileName))) {
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheetAt(2);
            Iterator<Row> rowIterator = sheet.iterator();
            Boolean startProcessing = false;
            Boolean endProcessing = false;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                String mfName = String.valueOf(row.getCell(1));

                if (mfName.contains("Short Term Trades Debt")) {
                    endProcessing = true;
                }
                if (startProcessing && !endProcessing) {
                    if (!mfName.equals("null") && !mfName.isEmpty() && !mfName.equals("Symbol") && !mfName.equals("Long Term Trades Equity")) {
                        equityMfList.add(mfName);
                    }

                }
                if (mfName.contains("Short Term Trades Equity")) {
                    startProcessing = true;
                }
            }
        }
        System.out.println(equityMfList);
        return equityMfList;
    }

    private void processEpfoTax() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Year:");
        Integer year = Integer.valueOf(formatNumber(scanner.nextLine()));
        System.out.print("Enter Previous Year Balance:");
        Integer previousBalance = Integer.valueOf(formatNumber(scanner.nextLine()));
        System.out.println("Enter Taxable Component April to March, Refer to EPFO passbook\n");
        System.out.print("April:");
        Integer _04Pf = Integer.valueOf(formatNumber(scanner.nextLine()));
        System.out.print("May:");
        Integer _05Pf = Integer.valueOf(formatNumber(scanner.nextLine()));
        System.out.print("June:");
        Integer _06Pf = Integer.valueOf(formatNumber(scanner.nextLine()));
        System.out.print("July:");
        Integer _07Pf = Integer.valueOf(formatNumber(scanner.nextLine()));
        System.out.print("August:");
        Integer _08Pf = Integer.valueOf(formatNumber(scanner.nextLine()));
        System.out.print("September:");
        Integer _09Pf = Integer.valueOf(formatNumber(scanner.nextLine()));
        System.out.print("October:");
        Integer _10Pf = Integer.valueOf(formatNumber(scanner.nextLine()));
        System.out.print("November:");
        Integer _11Pf = Integer.valueOf(formatNumber(scanner.nextLine()));
        System.out.print("December:");
        Integer _12Pf = Integer.valueOf(formatNumber(scanner.nextLine()));
        System.out.print("January:");
        Integer _01Pf = Integer.valueOf(formatNumber(scanner.nextLine()));
        System.out.print("February:");
        Integer _02Pf = Integer.valueOf(formatNumber(scanner.nextLine()));
        System.out.print("March:");
        Integer _03Pf = Integer.valueOf(formatNumber(scanner.nextLine()));
        List<Integer> pfList = List.of(_04Pf, _05Pf, _06Pf, _07Pf, _08Pf, _09Pf, _10Pf, _11Pf, _12Pf, _01Pf, _02Pf, _03Pf);
        int index = 11;
        List<Double> result = new ArrayList<>();
        Double totalInterest = 0.0;
        Integer total = 0;
        Float interestRate = epfInterest.get(year);
        for (Integer pfEntry : pfList) {
            Double curInterest = Math.floor(calculatePercentage(pfEntry, interestRate) * (index) / 12);
            result.add(curInterest);
            totalInterest = totalInterest + curInterest;
            total = pfEntry;
            index--;
        }
        double interestOnPrevBalance = Math.floor(calculatePercentage(previousBalance, interestRate));
        double newBalance = Math.floor(total + previousBalance + totalInterest);

        System.out.println("Taxable Interest, 10(11) first provision: " + totalInterest);
        System.out.println("Previous Balance Taxable Interest, 10(12) first provision: " + interestOnPrevBalance);
        System.out.println("New Balance: " + newBalance);

    }

    private String formatNumber(String number) {
        return number.strip().replaceAll(",", "");
    }

    private double calculatePercentage(double number, double percentage) {
        return (number * percentage) / 100;
    }

    private String getQuarter(LocalDate date) {
        int curYear = Year.now().getValue();
        LocalDate q0 = LocalDate.of(curYear - 1, 4, 1);
        LocalDate q1 = LocalDate.of(curYear - 1, 6, 15);
        LocalDate q2 = LocalDate.of(curYear - 1, 9, 15);
        LocalDate q3 = LocalDate.of(curYear - 1, 12, 15);
        LocalDate q4 = LocalDate.of(curYear, 3, 15);
        LocalDate q5 = LocalDate.of(curYear, 3, 31);
        if (date.isAfter(q0) && date.isBefore(q1)) {
            return Q1;
        }
        if (date.isAfter(q1) && date.isBefore(q2)) {
            return Q2;
        }
        if (date.isAfter(q2) && date.isBefore(q3)) {
            return Q3;
        }
        if (date.isAfter(q3) && date.isBefore(q4)) {
            return Q4;
        }
        if (date.isAfter(q4) && date.isBefore(q5)) {
            return Q5;
        }
        return "";
    }

    @SneakyThrows
    private void processDividend(String fileName) {
        List<List<String>> resultData = getDividendData(fileName);
        Map<String, Float> quarterlyDividendMap = new HashMap<>();
        float runningTotal = 0.0f;
        for (List<String> rowData : resultData) {
            //System.out.println(rowData);
            Float amount = Float.valueOf(rowData.get(5));
            runningTotal = runningTotal + amount;
            LocalDate divdendDate = LocalDate.parse(rowData.get(2));
            String quarter = getQuarter(divdendDate);
            switch (quarter) {
                case Q1: {
                    Float value = quarterlyDividendMap.getOrDefault(Q1, 0.0f);
                    value = value + amount;
                    quarterlyDividendMap.put(Q1, value);
                }
                break;
                case Q2: {
                    Float value = quarterlyDividendMap.getOrDefault(Q2, 0.0f);
                    value = value + amount;
                    quarterlyDividendMap.put(Q2, value);
                }
                break;
                case Q3: {
                    Float value = quarterlyDividendMap.getOrDefault(Q3, 0.0f);
                    value = value + amount;
                    quarterlyDividendMap.put(Q3, value);
                }
                break;
                case Q4: {
                    Float value = quarterlyDividendMap.getOrDefault(Q4, 0.0f);
                    value = value + amount;
                    quarterlyDividendMap.put(Q4, value);
                }
                break;
                case Q5: {
                    Float value = quarterlyDividendMap.getOrDefault(Q5, 0.0f);
                    value = value + amount;
                    quarterlyDividendMap.put(Q5, value);
                }
                break;
            }
        }
        System.out.println();
        System.out.println("Dividend Tax Breakup");
        System.out.println("Quarter: " + quarterlyDividendMap);
        System.out.println("Total: " + runningTotal);
        System.out.println();
    }

    @SneakyThrows
    private void processEquity(String fileName, List<String> equityMfList) {
        List<List<String>> resultData = getEquityStockData(fileName);

        //Processing equity/arbitrage mutual funds
        resultData.addAll(getMfData(fileName, false, equityMfList));

        Map<String, Float> longTermGainQuarterlyMap = new HashMap<>();
        Map<String, Float> shortTermGainQuarterlyMap = new HashMap<>();

        float runningLTCGProfit = 0.0f;
        float runningSTCGProfit = 0.0f;

        float ltcgFullValueConsideration = 0.0f;
        float ltcgCostAquisition = 0.0f;

        float stcgFullValueConsideration = 0.0f;
        float stcgCostAquisition = 0.0f;

        for (List<String> rowData : resultData) {
            //System.out.println(rowData);
            Float profit = Float.valueOf(rowData.get(7));
            LocalDate buyDate = LocalDate.parse(rowData.get(2));
            LocalDate exitDate = LocalDate.parse(rowData.get(3));
            Float periodOfHolding = Float.valueOf(rowData.get(8));
            if (periodOfHolding > EQ_LONG_TERM) {
                runningLTCGProfit = runningLTCGProfit + profit;
                ltcgFullValueConsideration = ltcgFullValueConsideration + Float.valueOf(rowData.get(6));
                ltcgCostAquisition = ltcgCostAquisition + Float.valueOf(rowData.get(5));
            } else {
                runningSTCGProfit = runningSTCGProfit + profit;
                stcgFullValueConsideration = stcgFullValueConsideration + Float.valueOf(rowData.get(6));
                stcgCostAquisition = +stcgCostAquisition + Float.valueOf(rowData.get(5));
            }
            String quarter = getQuarter(exitDate);
            switch (quarter) {
                case Q1:
                    if (periodOfHolding > EQ_LONG_TERM) {
                        Float value = longTermGainQuarterlyMap.getOrDefault(Q1, 0.0f);
                        value = value + profit;
                        longTermGainQuarterlyMap.put(Q1, value);
                    } else {
                        Float value = shortTermGainQuarterlyMap.getOrDefault(Q1, 0.0f);
                        value = value + profit;
                        shortTermGainQuarterlyMap.put(Q1, value);
                    }
                    break;
                case Q2:
                    if (periodOfHolding > EQ_LONG_TERM) {
                        Float value = longTermGainQuarterlyMap.getOrDefault(Q2, 0.0f);
                        value = value + profit;
                        longTermGainQuarterlyMap.put(Q2, value);
                    } else {
                        Float value = shortTermGainQuarterlyMap.getOrDefault(Q2, 0.0f);
                        value = value + profit;
                        shortTermGainQuarterlyMap.put(Q2, value);
                    }
                    break;
                case Q3:
                    if (periodOfHolding > EQ_LONG_TERM) {
                        Float value = longTermGainQuarterlyMap.getOrDefault(Q3, 0.0f);
                        value = value + profit;
                        longTermGainQuarterlyMap.put(Q3, value);
                    } else {
                        Float value = shortTermGainQuarterlyMap.getOrDefault(Q3, 0.0f);
                        value = value + profit;
                        shortTermGainQuarterlyMap.put(Q3, value);
                    }
                    break;
                case Q4:
                    if (periodOfHolding > EQ_LONG_TERM) {
                        Float value = longTermGainQuarterlyMap.getOrDefault(Q4, 0.0f);
                        value = value + profit;
                        longTermGainQuarterlyMap.put(Q4, value);
                    } else {
                        Float value = shortTermGainQuarterlyMap.getOrDefault(Q4, 0.0f);
                        value = value + profit;
                        shortTermGainQuarterlyMap.put(Q4, value);
                    }
                    break;
                case Q5:
                    if (periodOfHolding > EQ_LONG_TERM) {
                        Float value = longTermGainQuarterlyMap.getOrDefault(Q5, 0.0f);
                        value = value + profit;
                        longTermGainQuarterlyMap.put(Q5, value);
                    } else {
                        Float value = shortTermGainQuarterlyMap.getOrDefault(Q5, 0.0f);
                        value = value + profit;
                        shortTermGainQuarterlyMap.put(Q5, value);
                    }
                    break;
            }
        }

        System.out.println();
        System.out.println("Equity LTCG Tax (10% after 1 Lakh) Breakup");
        System.out.println("Full Value of Consideration (Total Sale Value): " + ltcgFullValueConsideration);
        System.out.println("Cost of acquisition: " + ltcgCostAquisition);
        System.out.println("Profit: " + (ltcgFullValueConsideration - ltcgCostAquisition));
        System.out.println("Quarter: " + longTermGainQuarterlyMap);
        System.out.println("Total: " + runningLTCGProfit);
        System.out.println();

        System.out.println();
        System.out.println("Equity STCG Tax (15%) Breakup");
        System.out.println("Full Value of Consideration (Total Sale Value): " + stcgFullValueConsideration);
        System.out.println("Cost of acquisition: " + stcgCostAquisition);
        System.out.println("Profit: " + (stcgFullValueConsideration - stcgCostAquisition));
        System.out.println("Quarter: " + shortTermGainQuarterlyMap);
        System.out.println("Total: " + runningSTCGProfit);
        System.out.println();
    }

    @SneakyThrows
    private void processDebt(String fileName, List<String> equityMfList) {
        List<List<String>> resultData = getMfData(fileName, true, equityMfList);
        LocalDate LAST_DATE_OF_DEBT_INDEXATION = LocalDate.of(2023, 3, 31);
        Map<String, Float> longTermGainQuarterlyMap = new HashMap<>();
        Map<String, Float> shortTermGainQuarterlyMap = new HashMap<>();

        float runningLTCGProfit = 0.0f;
        float runningSTCGProfit = 0.0f;

        float ltcgFullValueConsideration = 0.0f;
        float ltcgCostAquisition = 0.0f;

        float stcgFullValueConsideration = 0.0f;
        float stcgCostAquisition = 0.0f;

        for (List<String> rowData : resultData) {
            //System.out.println(rowData);
            Float profit = Float.valueOf(rowData.get(7));
            LocalDate buyDate = LocalDate.parse(rowData.get(2));
            LocalDate exitDate = LocalDate.parse(rowData.get(3));
            Float periodOfHolding = Float.valueOf(rowData.get(8));
            if (periodOfHolding > DEBT_LONG_TERM && exitDate.isBefore(LAST_DATE_OF_DEBT_INDEXATION)) {
                runningLTCGProfit = runningLTCGProfit + profit;
                ltcgFullValueConsideration = ltcgFullValueConsideration + Float.valueOf(rowData.get(6));
                ltcgCostAquisition = ltcgCostAquisition + Float.valueOf(rowData.get(5));
            } else {
                runningSTCGProfit = runningSTCGProfit + profit;
                stcgFullValueConsideration = stcgFullValueConsideration + Float.valueOf(rowData.get(6));
                stcgCostAquisition = +stcgCostAquisition + Float.valueOf(rowData.get(5));
            }

            String quarter = getQuarter(exitDate);
            switch (quarter) {
                case Q1: {
                    if (periodOfHolding > DEBT_LONG_TERM && exitDate.isBefore(LAST_DATE_OF_DEBT_INDEXATION)) {
                        Float value = longTermGainQuarterlyMap.getOrDefault(Q1, 0.0f);
                        value = value + profit;
                        longTermGainQuarterlyMap.put(Q1, value);
                    } else {
                        Float value = shortTermGainQuarterlyMap.getOrDefault(Q1, 0.0f);
                        value = value + profit;
                        shortTermGainQuarterlyMap.put(Q1, value);
                    }
                }
                break;
                case Q2: {
                    if (periodOfHolding > DEBT_LONG_TERM && exitDate.isBefore(LAST_DATE_OF_DEBT_INDEXATION)) {
                        Float value = longTermGainQuarterlyMap.getOrDefault(Q2, 0.0f);
                        value = value + profit;
                        longTermGainQuarterlyMap.put(Q2, value);
                    } else {
                        Float value = shortTermGainQuarterlyMap.getOrDefault(Q2, 0.0f);
                        value = value + profit;
                        shortTermGainQuarterlyMap.put(Q2, value);
                    }
                }
                break;
                case Q3: {
                    if (periodOfHolding > DEBT_LONG_TERM && exitDate.isBefore(LAST_DATE_OF_DEBT_INDEXATION)) {
                        Float value = longTermGainQuarterlyMap.getOrDefault(Q3, 0.0f);
                        value = value + profit;
                        longTermGainQuarterlyMap.put(Q3, value);
                    } else {
                        Float value = shortTermGainQuarterlyMap.getOrDefault(Q3, 0.0f);
                        value = value + profit;
                        shortTermGainQuarterlyMap.put(Q3, value);
                    }
                }
                break;
                case Q4: {
                    if (periodOfHolding > DEBT_LONG_TERM && exitDate.isBefore(LAST_DATE_OF_DEBT_INDEXATION)) {
                        Float value = longTermGainQuarterlyMap.getOrDefault(Q4, 0.0f);
                        value = value + profit;
                        longTermGainQuarterlyMap.put(Q4, value);
                    } else {
                        Float value = shortTermGainQuarterlyMap.getOrDefault(Q4, 0.0f);
                        value = value + profit;
                        shortTermGainQuarterlyMap.put(Q4, value);
                    }
                }
                break;
                case Q5: {
                    if (periodOfHolding > DEBT_LONG_TERM && exitDate.isBefore(LAST_DATE_OF_DEBT_INDEXATION)) {
                        Float value = longTermGainQuarterlyMap.getOrDefault(Q5, 0.0f);
                        value = value + profit;
                        longTermGainQuarterlyMap.put(Q5, value);
                    } else {
                        Float value = shortTermGainQuarterlyMap.getOrDefault(Q5, 0.0f);
                        value = value + profit;
                        shortTermGainQuarterlyMap.put(Q5, value);
                    }
                }
                break;

            }
        }

        System.out.println();
        System.out.println("Debt LTCG (20%) Tax Breakup");
        System.out.println("Full Value of Consideration (Total Sale Value): " + ltcgFullValueConsideration);
        System.out.println("Cost of acquisition (without indexation): " + ltcgCostAquisition);
        System.out.println("Profit: " + (ltcgFullValueConsideration - ltcgCostAquisition));
        System.out.println("Quarter: " + longTermGainQuarterlyMap);
        System.out.println("Total: " + runningLTCGProfit);
        System.out.println();

        System.out.println();
        System.out.println("Debt STCG Tax (Slab Rate) Breakup");
        System.out.println("Full Value of Consideration (Total Sale Value): " + stcgFullValueConsideration);
        System.out.println("Cost of acquisition: " + stcgCostAquisition);
        System.out.println("Profit: " + (stcgFullValueConsideration - stcgCostAquisition));
        System.out.println("Quarter: " + shortTermGainQuarterlyMap);
        System.out.println("Total: " + runningSTCGProfit);
        System.out.println();

    }

    @SneakyThrows
    private List<List<String>> getEquityStockData(String fileName) {
        List<List<String>> resultData = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(new File(fileName))) {
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            Boolean startProcessing = false;
            Boolean endProcessing = false;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Iterator<Cell> cellIterator = row.cellIterator();
                List<String> rowData = new ArrayList<>();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    switch (cell.getCellType()) {
                        case CellType.NUMERIC:
                            rowData.add(String.valueOf(cell.getNumericCellValue()));
                            break;
                        case CellType.STRING:
                            rowData.add(cell.getStringCellValue());
                            break;
                    }
                }
                if (rowData.contains("Equity - Buyback")) {
                    endProcessing = true;
                }
                if (startProcessing && !endProcessing) {
                    if (!rowData.isEmpty() && !rowData.get(0).equals("Symbol")) {
                        resultData.add(rowData);
                        //System.out.println(rowData);
                    }
                }
                if (rowData.contains("Equity")) {
                    startProcessing = true;
                }
            }
        }
        return resultData;
    }

    @SneakyThrows
    private List<List<String>> getMfData(String fileName, Boolean debtMf, List<String> equityMfList) {
        List<List<String>> resultData = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(new File(fileName))) {
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            Boolean startProcessing = false;
            Boolean endProcessing = false;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Iterator<Cell> cellIterator = row.cellIterator();
                List<String> rowData = new ArrayList<>();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    switch (cell.getCellType()) {
                        case CellType.NUMERIC:
                            rowData.add(String.valueOf(cell.getNumericCellValue()));
                            break;
                        case CellType.STRING:
                            rowData.add(cell.getStringCellValue());
                            break;
                    }
                }
                if (rowData.contains("F&O")) {
                    endProcessing = true;
                }
                if (startProcessing && !endProcessing) {
                    if (!rowData.isEmpty() && !rowData.get(0).equals("Symbol")) {
                        if (debtMf) {
                            //debt mutual fund
                            if (!equityMfList.contains(rowData.get(0))) {
                                resultData.add(rowData);
                                //System.out.println(rowData);
                            }
                        } else {
                            //equity mutual fund
                            if (equityMfList.contains(rowData.get(0))) {
                                resultData.add(rowData);
                                //System.out.println(rowData);
                            }
                        }
                    }
                }
                if (rowData.contains("Mutual Funds")) {
                    startProcessing = true;
                }
            }
        }
        return resultData;
    }

    @SneakyThrows
    private List<List<String>> getDividendData(String fileName) {
        List<List<String>> resultData = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(new File(fileName))) {
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            Boolean startProcessing = false;
            Boolean endProcessing = false;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Iterator<Cell> cellIterator = row.cellIterator();
                List<String> rowData = new ArrayList<>();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    switch (cell.getCellType()) {
                        case CellType.NUMERIC:
                            rowData.add(String.valueOf(cell.getNumericCellValue()));
                            break;
                        case CellType.STRING:
                            rowData.add(cell.getStringCellValue());
                            break;
                    }
                }
                if (rowData.contains("Total Dividend Amount")) {
                    endProcessing = true;
                }
                if (startProcessing && !endProcessing) {
                    if (!rowData.isEmpty()) {
                        resultData.add(rowData);
                        //System.out.println(rowData);
                    }
                }
                if (rowData.contains("Symbol")) {
                    startProcessing = true;
                }
            }
        }
        return resultData;
    }

}

