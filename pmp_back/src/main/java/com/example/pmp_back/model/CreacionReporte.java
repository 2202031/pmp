package com.example.pmp_back.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

// Reporte técnico que llena el Supervisor (datos de facturación + 4 fotos de evidencia).
// Vinculado a una supervisión específica mediante 'folio' (columna agregada vía migración).
@Entity
@Table(name = "creacion_reporte")
public class CreacionReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer id;

    @Column(name = "folio", length = 50)
    private String folio;

    @Column(name = "anio_notificacion")
    private Integer anioNotificacion;

    private Float kwh;

    private BigDecimal importe;

    @Column(length = 100)
    private String rpu;

    @Column(name = "id_usuarios")
    private Integer idUsuarios;

    @Column(name = "numero_corte", length = 100)
    private String numeroCorte;

    @Column(length = 100)
    private String tarifa;

    @Column(name = "status_servicio", length = 100)
    private String statusServicio;

    @Lob
    @Column(name = "foto_corte", columnDefinition = "LONGBLOB")
    private byte[] fotoCorte;

    @Lob
    @Column(name = "foto_fachada", columnDefinition = "LONGBLOB")
    private byte[] fotoFachada;

    @Lob
    @Column(name = "foto_medidor", columnDefinition = "LONGBLOB")
    private byte[] fotoMedidor;

    @Lob
    @Column(name = "foto_selfi", columnDefinition = "LONGBLOB")
    private byte[] fotoSelfi;

    // --- GETTERS Y SETTERS ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }

    public Integer getAnioNotificacion() { return anioNotificacion; }
    public void setAnioNotificacion(Integer anioNotificacion) { this.anioNotificacion = anioNotificacion; }

    public Float getKwh() { return kwh; }
    public void setKwh(Float kwh) { this.kwh = kwh; }

    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }

    public String getRpu() { return rpu; }
    public void setRpu(String rpu) { this.rpu = rpu; }

    public Integer getIdUsuarios() { return idUsuarios; }
    public void setIdUsuarios(Integer idUsuarios) { this.idUsuarios = idUsuarios; }

    public String getNumeroCorte() { return numeroCorte; }
    public void setNumeroCorte(String numeroCorte) { this.numeroCorte = numeroCorte; }

    public String getTarifa() { return tarifa; }
    public void setTarifa(String tarifa) { this.tarifa = tarifa; }

    public String getStatusServicio() { return statusServicio; }
    public void setStatusServicio(String statusServicio) { this.statusServicio = statusServicio; }

    public byte[] getFotoCorte() { return fotoCorte; }
    public void setFotoCorte(byte[] fotoCorte) { this.fotoCorte = fotoCorte; }

    public byte[] getFotoFachada() { return fotoFachada; }
    public void setFotoFachada(byte[] fotoFachada) { this.fotoFachada = fotoFachada; }

    public byte[] getFotoMedidor() { return fotoMedidor; }
    public void setFotoMedidor(byte[] fotoMedidor) { this.fotoMedidor = fotoMedidor; }

    public byte[] getFotoSelfi() { return fotoSelfi; }
    public void setFotoSelfi(byte[] fotoSelfi) { this.fotoSelfi = fotoSelfi; }
}
