package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordRequest;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface RegisterRefuelingRecordUseCase {
    RefuelingRecordResponse registrar(RefuelingRecordRequest request, MultipartFile factura, Long responsableId);
    Page<RefuelingRecordResponse> listar(Pageable pageable, Boolean activo);
    RefuelingRecordResponse actualizar(Long id, RefuelingRecordRequest request, MultipartFile factura);
    void eliminar(Long id);
}
