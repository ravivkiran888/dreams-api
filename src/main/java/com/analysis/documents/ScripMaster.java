package com.analysis.documents;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.analysis.constants.Constants;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Document(collection = Constants.SCRIP_SYMBOL_EQ_COLLECTION)
@Setter
@Getter
public class ScripMaster {

    @Id
    @JsonProperty("_id")
    private String id;

    @Field("ScripCode")
    @JsonProperty("ScripCode")
    private String scripCode;

    @Field("Symbol")
    @JsonProperty("Symbol")
    private String symbol;

    @Field("Sector")
    @JsonProperty("Sector")
    private String sector;
    
}
