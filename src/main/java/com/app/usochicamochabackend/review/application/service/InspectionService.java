package com.app.usochicamochabackend.review.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.mapper.ImagesMapper;
import com.app.usochicamochabackend.mapper.InspectionMapper;
import com.app.usochicamochabackend.mapper.MachineMapper;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import com.app.usochicamochabackend.review.application.dto.*;
import com.app.usochicamochabackend.review.application.port.*;
import com.app.usochicamochabackend.review.infrastructure.entity.ImageEntity;
import com.app.usochicamochabackend.review.infrastructure.entity.InspectionEntity;
import com.app.usochicamochabackend.review.infrastructure.repository.ImageRepository;
import com.app.usochicamochabackend.review.infrastructure.repository.InspectionRepository;
import com.app.usochicamochabackend.review.application.dto.ExpirationNotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InspectionService implements CreateInspectionOnlyDataUseCase, SaveInspectionImageUseCase, GetInspectionByIdUseCase, GetInspectionImagesUseCase, GetAllInspectionsWithoutImagesUseCase, GetAllInspectionsForExportUseCase, UpdateInspectionHourMeterUseCase {

    private final NotificationService notificationService;
    private final InspectionRepository inspectionRepository;
    private final UserRepositoryJpa userRepository;
    private final MachineRepository machineRepository;
    private final ImageRepository imageRepository;
    private final SaveActionUseCase saveActionUseCase;

    @Override
    public InspectionFormResponse createInspectionOnlyData(InspectionFormRequest request) {
        InspectionEntity entity = InspectionMapper.toEntityWithoutOrdersAndImages(
                request, userRepository, machineRepository);

        // First, check if an inspection with the same UUID already exists
        Optional<InspectionEntity> existingByUUID = inspectionRepository.findByUUID(entity.getUUID());
        if (existingByUUID.isPresent()) {
            return InspectionMapper.toDto(existingByUUID.get());
        }

        // Check for identical inspection in the last 12 hours
        LocalDateTime twelveHoursAgo = LocalDateTime.now().minusHours(12);
        List<InspectionEntity> recentInspections = inspectionRepository.findByMachineIdAndUserIdAndDateStampAfter(
                request.machineId(), request.userId(), twelveHoursAgo);

        for (InspectionEntity existing : recentInspections) {
            if (isIdenticalInspection(entity, existing)) {
                return InspectionMapper.toDto(existing);
            }
        }

        InspectionEntity saved = inspectionRepository.save(entity);
        InspectionFormResponse inspectionResponse = InspectionMapper.toDto(saved);

        if (Boolean.TRUE.equals(saved.getUnexpected())) {
            notificationService.notifyInspection(inspectionResponse.toString());
        }

        MachineEntity machine = saved.getMachine();

        if (isExpiringSoon(machine.getSoat())) {
            ExpirationNotificationDTO soatNotification = new ExpirationNotificationDTO(
                    "SOAT",
                    "⚠️ El SOAT de la máquina '" + machine.getName() + "' vence pronto",
                    MachineMapper.toResponse(machine)
            );
            notificationService.notifySoatRunt(soatNotification.toString());
        }

        if (isExpiringSoon(machine.getRunt())) {
            ExpirationNotificationDTO runtNotification = new ExpirationNotificationDTO(
                    "RUNT",
                    "⚠️ El RUNT de la máquina '" + machine.getName() + "' vence pronto",
                    MachineMapper.toResponse(machine)
            );
            notificationService.notifySoatRunt(runtNotification.toString());
        }

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() + " hizo una inspeccion a la maquina " + machine.getName());


        return inspectionResponse;
    }

    private boolean isExpiringSoon(LocalDate date) {
        return date != null && !date.isBefore(LocalDate.now()) &&
                !date.isAfter(LocalDate.now().plusDays(15));
    }

    private boolean isIdenticalInspection(InspectionEntity newInspection, InspectionEntity existing) {
        return newInspection.getMachine().getId().equals(existing.getMachine().getId()) &&
                newInspection.getUser().getId().equals(existing.getUser().getId()) &&
                Objects.equals(newInspection.getUnexpected(), existing.getUnexpected()) &&
                Objects.equals(newInspection.getHourMeter(), existing.getHourMeter()) &&
                Objects.equals(newInspection.getLeakStatus(), existing.getLeakStatus()) &&
                Objects.equals(newInspection.getBrakeStatus(), existing.getBrakeStatus()) &&
                Objects.equals(newInspection.getBeltsPulleysStatus(), existing.getBeltsPulleysStatus()) &&
                Objects.equals(newInspection.getTireLanesStatus(), existing.getTireLanesStatus()) &&
                Objects.equals(newInspection.getCarIgnitionStatus(), existing.getCarIgnitionStatus()) &&
                Objects.equals(newInspection.getElectricalStatus(), existing.getElectricalStatus()) &&
                Objects.equals(newInspection.getMechanicalStatus(), existing.getMechanicalStatus()) &&
                Objects.equals(newInspection.getTemperatureStatus(), existing.getTemperatureStatus()) &&
                Objects.equals(newInspection.getOilStatus(), existing.getOilStatus()) &&
                Objects.equals(newInspection.getHydraulicStatus(), existing.getHydraulicStatus()) &&
                Objects.equals(newInspection.getCoolantStatus(), existing.getCoolantStatus()) &&
                Objects.equals(newInspection.getStructuralStatus(), existing.getStructuralStatus()) &&
                Objects.equals(newInspection.getExpirationDateFireExtinguisher(), existing.getExpirationDateFireExtinguisher()) &&
                Objects.equals(newInspection.getObservations(), existing.getObservations()) &&
                Objects.equals(newInspection.getGreasingAction(), existing.getGreasingAction()) &&
                Objects.equals(newInspection.getGreasingObservations(), existing.getGreasingObservations());
    }

    @Override
    public ImageDTO saveInspectionImage(Long inspectionId, MultipartFile image) throws IOException {
        if (image.getSize() > 1048576L) {
            throw new IllegalArgumentException("Image size exceeds 1000kb limit");
        }

        InspectionEntity inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found"));

        String uuid = inspection.getUUID();

        Path uploadsPath = Paths.get("uploads");
        if (!Files.exists(uploadsPath)) {
            Files.createDirectories(uploadsPath);
        }

        Path inspectionFolder = uploadsPath.resolve(uuid);
        if (!Files.exists(inspectionFolder)) {
            Files.createDirectories(inspectionFolder);
        }

        if (inspection.getImages() != null && !inspection.getImages().isEmpty()) {
            ImageEntity lastImage = inspection.getImages().get(inspection.getImages().size() - 1);
            Path lastImagePath = Paths.get(lastImage.getUrl());

            if (Files.exists(lastImagePath)) {
                if (isDuplicateHalfImage(image, lastImagePath)) {
                    throw new IllegalArgumentException("Duplicate image detected");
                }
            }
        }

        int nextIndex = inspection.getImages() != null ? inspection.getImages().size() + 1 : 1;
        String extension = getFileExtension(image);

        String filename = uuid + "-" + nextIndex + extension;
        Path filePath = inspectionFolder.resolve(filename);

        Files.write(filePath, image.getBytes());

        ImageEntity img = new ImageEntity();
        img.setUrl(filePath.toString());
        img.setInspection(inspection);

        if (inspection.getImages() == null) {
            inspection.setImages(new ArrayList<>());
        }
        inspection.getImages().add(img);

        InspectionEntity inspectionEntity = inspectionRepository.save(inspection);
        List<ImageEntity> images = inspectionEntity.getImages();
        ImageEntity lastImage = images.get(images.size() - 1);
        img.setId(lastImage.getId());

        return ImagesMapper.toDto(img);
    }

    private boolean isDuplicateHalfImage(MultipartFile newImage, Path lastImagePath) throws IOException {
        BufferedImage newImg = ImageIO.read(newImage.getInputStream());
        BufferedImage lastImg = ImageIO.read(lastImagePath.toFile());

        if (newImg == null || lastImg == null) return false;

        int width = Math.min(newImg.getWidth(), lastImg.getWidth());
        int height = Math.min(newImg.getHeight(), lastImg.getHeight());

        BufferedImage newHalf = newImg.getSubimage(0, 0, width, height / 2);
        BufferedImage lastHalf = lastImg.getSubimage(0, 0, width, height / 2);

        for (int y = 0; y < newHalf.getHeight(); y++) {
            for (int x = 0; x < newHalf.getWidth(); x++) {
                if (newHalf.getRGB(x, y) != lastHalf.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public InspectionDTO getInspectionById(Long inspectionId) {
        InspectionEntity inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found"));

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() + " ha observado una inspeccion realizada a la maquina " + inspection.getMachine().getName() + " realizada el dia " + inspection.getDateStamp().toLocalDate());


        return InspectionMapper.toDtoWithImagesAndOrders(inspection);
    }

    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String contentType = file.getContentType();
        if ("image/png".equals(contentType)) {
            return ".png";
        } else if ("image/jpeg".equals(contentType)) {
            return ".jpg";
        }
        return ".bin"; // fallback
    }

    @Override
    public List<ImageDTO> getAllImagesByInspectionId(Long inspectionId) {
        InspectionEntity inspection = inspectionRepository.findById(inspectionId).orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));
        List<ImageEntity> imageEntitiesList = imageRepository.findByInspectionId(inspectionId);

        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save("El usuario " + userPrincipal.username() + " ha observado todas las imagenes de la inspeccion realizada a la maquina " + inspection.getMachine().getName() + " realizada el dia " + inspection.getDateStamp().toLocalDate());
        }

        return ImagesMapper.toDtoList(imageEntitiesList);
    }

    @Override
    public Page<InspectionFormResponse> getAllInspectionsWithoutImages(Pageable pageable) {
        Page<InspectionEntity> inspections = inspectionRepository.findAll(pageable);

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() + " ha observado todas las inspecciones el dia " + LocalDateTime.now());


        return inspections.map(InspectionMapper::toDto);
    }

    @Override
    public List<InspectionEntity> getAllInspectionsForExport() {
        return inspectionRepository.findAllWithMachineAndUser();
    }

    @Override
    public void updateHourMeter(Long machineId, Double newHourMeter) {
        InspectionEntity last = inspectionRepository.getLastInspection(machineId);
        if (last == null) throw new ResourceNotFoundException("No inspection found for machine " + machineId);
        last.setHourMeter(newHourMeter);
        inspectionRepository.save(last);

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        saveActionUseCase.save("El usuario " + userPrincipal.username() + " corrigió el horómetro de la máquina " + last.getMachine().getName() + " a " + newHourMeter);
    }
}