UPDATE system_build_versions
SET dispatch_estimate = '7–14 business days after cleared payment'
WHERE dispatch_estimate IS DISTINCT FROM '7–14 business days after cleared payment';
