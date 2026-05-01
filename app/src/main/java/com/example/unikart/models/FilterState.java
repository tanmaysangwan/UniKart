package com.example.unikart.models;

public class FilterState {
    public enum SortOrder { 
        NONE, 
        PRICE_LOW_HIGH, 
        PRICE_HIGH_LOW,
        RATING_HIGH_LOW,
        RATING_LOW_HIGH,
        MOST_REVIEWED,
        RECOMMENDED
    }
    
    public enum RatingFilter {
        ALL,
        HIGHLY_RATED,    // 4.5+
        GOOD_SELLERS,    // 4.0+
        TRUSTED_SELLERS, // 10+ reviews
        NEW_SELLERS      // 0 reviews
    }

    private String typeFilter;      // null = all, "BUY", "RENT"
    private String categoryFilter;  // null = all
    private String conditionFilter; // null = all
    private SortOrder sortOrder;
    private String searchQuery;
    private RatingFilter ratingFilter;

    public FilterState() {
        sortOrder = SortOrder.NONE;
        ratingFilter = RatingFilter.ALL;
    }

    public String getTypeFilter()               { return typeFilter; }
    public void setTypeFilter(String v)         { this.typeFilter = v; }

    public String getCategoryFilter()           { return categoryFilter; }
    public void setCategoryFilter(String v)     { this.categoryFilter = v; }

    public String getConditionFilter()          { return conditionFilter; }
    public void setConditionFilter(String v)    { this.conditionFilter = v; }

    public SortOrder getSortOrder()             { return sortOrder; }
    public void setSortOrder(SortOrder v)       { this.sortOrder = v; }

    public String getSearchQuery()              { return searchQuery; }
    public void setSearchQuery(String v)        { this.searchQuery = v; }
    
    public RatingFilter getRatingFilter()       { return ratingFilter; }
    public void setRatingFilter(RatingFilter v) { this.ratingFilter = v; }

    public boolean isActive() {
        return typeFilter != null || categoryFilter != null || conditionFilter != null
                || sortOrder != SortOrder.NONE
                || ratingFilter != RatingFilter.ALL
                || (searchQuery != null && !searchQuery.isEmpty());
    }

    public void reset() {
        typeFilter = null;
        categoryFilter = null;
        conditionFilter = null;
        sortOrder = SortOrder.NONE;
        ratingFilter = RatingFilter.ALL;
        searchQuery = null;
    }
}
