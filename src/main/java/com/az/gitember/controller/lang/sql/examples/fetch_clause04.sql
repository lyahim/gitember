SELECT
    (SELECT ID_ FROM TP_STUFF WHERE TEST='1' FETCH FIRST ROW ONLY) AS MWST_ID
FROM ETL_MD_RAW_ADM ADM
WHERE ADM.STATUS_ART_ID = 0;