package org.wikipedia.ro.java;

import java.io.IOException;

import org.junit.Test;
import org.wikibase.Wikibase;
import org.wikibase.WikibaseException;
import org.wikibase.data.Entity;
import org.wikipedia.ro.cache.WikidataEntitiesCache;
import org.wikipedia.ro.java.lister.generators.SettlementListsGenerator;

public class TestSettlementsListGenerator {

    @Test
    public void testGenerate() throws IOException, WikibaseException {
        String countyId = "Q179559";
        
        Wikibase wikibase = new Wikibase();
        wikibase.setThrottle(70000);
        WikidataEntitiesCache wikidataEntitiesCache = new WikidataEntitiesCache(wikibase);
        SettlementListsGenerator sut = new SettlementListsGenerator(wikidataEntitiesCache);
        
        Entity countyEnt = wikibase.getWikibaseItemById(countyId);
        
        String aradCountyList = sut.generateListContent(countyEnt);
        
        System.out.println(aradCountyList);
        
    }
}