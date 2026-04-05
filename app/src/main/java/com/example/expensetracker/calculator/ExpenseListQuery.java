package com.example.expensetracker.calculator;

public class ExpenseListQuery {

    public enum SortType {
        DATE_DESC,
        DATE_ASC,
        AMOUNT_DESC,
        AMOUNT_ASC
    }

    private String memberIdFilter;
    private SortType sortType;

    public ExpenseListQuery(String memberIdFilter, SortType sortType) {
        this.memberIdFilter = memberIdFilter;
        this.sortType = sortType;
    }

    public String getMemberIdFilter() {
        return memberIdFilter;
    }

    public SortType getSortType() {
        return sortType;
    }
}
