package com.example.expensetracker.calculator;

public class ExpenseListQuery {

    public enum SortType {
        DATE_DESC,
        DATE_ASC,
        AMOUNT_DESC,
        AMOUNT_ASC
    }

    public enum TypeFilter {
        GROUP,
        INDIVIDUAL
    }

    private String memberIdFilter;
    private TypeFilter typeFilter;
    private SortType sortType;

    public ExpenseListQuery(String memberIdFilter, SortType sortType) {
        this(memberIdFilter, null, sortType);
    }

    public ExpenseListQuery(String memberIdFilter, TypeFilter typeFilter, SortType sortType) {
        this.memberIdFilter = memberIdFilter;
        this.typeFilter = typeFilter;
        this.sortType = sortType;
    }

    public String getMemberIdFilter() {
        return memberIdFilter;
    }

    public SortType getSortType() {
        return sortType;
    }

    public TypeFilter getTypeFilter() {
        return typeFilter;
    }
}
