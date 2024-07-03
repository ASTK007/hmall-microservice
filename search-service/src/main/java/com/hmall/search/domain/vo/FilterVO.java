package com.hmall.search.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class FilterVO {

    @JsonProperty("category")
    private List<String> category;
    @JsonProperty("brand")
    private List<String> brand;
}
