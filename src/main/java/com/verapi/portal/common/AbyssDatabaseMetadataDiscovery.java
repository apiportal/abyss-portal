/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 10 2018
 *
 */

package com.verapi.portal.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbyssDatabaseMetadataDiscovery {
    private static final AbyssDatabaseMetadataDiscovery INSTANCE = new AbyssDatabaseMetadataDiscovery();
    private Map<String, List<AbyssDatabaseMetadata>> abyssDatabaseMetadataMap = new HashMap<>();

    private AbyssDatabaseMetadataDiscovery() {
    }

    public static AbyssDatabaseMetadataDiscovery getInstance() {
        return INSTANCE;
    }

    public Map<String, List<AbyssDatabaseMetadata>> getAbyssDatabaseMetadataMap() {
        return abyssDatabaseMetadataMap;
    }

    public void setAbyssDatabaseMetadataMap(Map<String, List<AbyssDatabaseMetadata>> abyssDatabaseMetadataMap) {
        this.abyssDatabaseMetadataMap = abyssDatabaseMetadataMap;
    }

    public List<AbyssDatabaseMetadata> getTableMetadata(String table) {
        return abyssDatabaseMetadataMap.get(table);
    }

    public void addTableMetada(String table, List<AbyssDatabaseMetadata> tableMetaData) {
        abyssDatabaseMetadataMap.put(table, tableMetaData);
    }

    public String getTableColumnList(String table) {
        final String[] commaSeperatedColumnList = new String[1];
        commaSeperatedColumnList[0] = "";
        abyssDatabaseMetadataMap.get(table).forEach(abyssDatabaseMetadata -> {
                    if (!abyssDatabaseMetadata.isPrivate && !abyssDatabaseMetadata.isWriteOnly)
                        if (abyssDatabaseMetadata.isJsonb)
                            commaSeperatedColumnList[0] += table + "." + abyssDatabaseMetadata.COLUMN_NAME + "::JSON,\n";
                        else
                            commaSeperatedColumnList[0] += table + "." + abyssDatabaseMetadata.COLUMN_NAME + ",\n";
                }
        );
        commaSeperatedColumnList[0] = commaSeperatedColumnList[0].substring(0, commaSeperatedColumnList[0].length() - 2); //truncate ",\n" chars

        return commaSeperatedColumnList[0];

/*
        String returnStr = "";
        for (String s : commaSeperatedColumnList) {
            returnStr += s;
        }
        return returnStr;
*/
    }

    public AbyssDatabaseMetadata getColumnMetadata(String table, String column) {
        final AbyssDatabaseMetadata[] columnMetadata = new AbyssDatabaseMetadata[1];
        abyssDatabaseMetadataMap.get(table).forEach(abyssDatabaseMetadata -> {
            if (abyssDatabaseMetadata.COLUMN_NAME.equals(column))
                columnMetadata[0] = abyssDatabaseMetadata;
        });
        return columnMetadata[0];
    }
}
