DELETE FROM md_inland_por;
DELETE FROM md_global_port;
DELETE FROM md_data_sync_meta WHERE sync_key = 'global_port_unlocode';
SELECT COUNT(*) AS port_count FROM md_global_port;
