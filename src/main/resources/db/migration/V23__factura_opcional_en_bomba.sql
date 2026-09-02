-- Un tanqueo en BOMBA exigía precio_unitario Y factura (V20). El usuario reportó que
-- el mensaje de error confundía (mencionaba ambos campos aunque solo faltara la
-- factura) y pidió que la factura pase a ser opcional — en la operación real no
-- siempre se tiene la factura a mano al momento de registrar el tanqueo. Solo
-- precio_unitario sigue siendo obligatorio.
ALTER TABLE refueling_records DROP CONSTRAINT refueling_records_check1;
ALTER TABLE refueling_records ADD CONSTRAINT refueling_records_check1
  CHECK (lugar <> 'BOMBA' OR precio_unitario IS NOT NULL);
