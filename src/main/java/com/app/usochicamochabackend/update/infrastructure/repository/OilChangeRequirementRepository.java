package com.app.usochicamochabackend.update.infrastructure.repository;

import com.app.usochicamochabackend.update.infrastructure.entity.OilChangeRequirementEntity;
import com.app.usochicamochabackend.update.infrastructure.entity.OilType;
import com.app.usochicamochabackend.update.infrastructure.entity.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para acceder a los requisitos de cambio de aceite
 */
@Repository
public interface OilChangeRequirementRepository extends JpaRepository<OilChangeRequirementEntity, Long> {

    /**
     * Encuentra un requisito específico por tipo de aceite y tipo de activo
     */
    Optional<OilChangeRequirementEntity> findByOilTypeAndAssetTypeAndKmRangeAndActive(
        OilType oilType,
        AssetType assetType,
        Integer kmRange,
        Boolean active
    );

    /**
     * Encuentra todos los requisitos para un tipo de aceite específico
     */
    List<OilChangeRequirementEntity> findByOilTypeAndActive(OilType oilType, Boolean active);

    /**
     * Encuentra todos los requisitos para un tipo de activo específico
     */
    List<OilChangeRequirementEntity> findByAssetTypeAndActive(AssetType assetType, Boolean active);

    /**
     * Encuentra todos los requisitos activos
     */
    List<OilChangeRequirementEntity> findByActiveTrue();

    /**
     * Consulta para listar opciones de cambio de aceite por tipo de activo
     */
    @Query("SELECT r FROM OilChangeRequirementEntity r WHERE r.assetType = :assetType AND r.active = true ORDER BY r.oilType, r.kmRange DESC NULLS LAST, r.hourRange DESC NULLS LAST")
    List<OilChangeRequirementEntity> findByAssetTypeForDisplay(@Param("assetType") AssetType assetType);

    /**
     * Encuentra un requisito para maquinaria por horometro
     */
    Optional<OilChangeRequirementEntity> findByAssetTypeAndOilTypeAndHourRangeAndActive(
        AssetType assetType,
        OilType oilType,
        Integer hourRange,
        Boolean active
    );
}
