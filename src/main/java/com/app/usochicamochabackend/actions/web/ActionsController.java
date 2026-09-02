package com.app.usochicamochabackend.actions.web;

import com.app.usochicamochabackend.actions.application.port.GetAllActionsByUserIdUseCase;
import com.app.usochicamochabackend.actions.application.port.GetAllActionsUseCase;
import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.actions.infrastructure.entity.ActionEntity;
import com.app.usochicamochabackend.actions.infrastructure.repository.ActionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/actions", "/api/v1/actions"})
@RequiredArgsConstructor
public class ActionsController {

    private final GetAllActionsUseCase getAllActionsUseCase;
    private final GetAllActionsByUserIdUseCase allActionsByUserIdUseCase;
    private final GetAllActionsByUserIdUseCase getAllActionsByUserIdUseCase;

    @GetMapping
    @Operation(
            summary = "Get all actions (paginated)",
            description = "Retrieves all actions in the system with pagination. " +
                    "This action will also log that the user retrieved the list."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved actions",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<Page<ActionEntity>> getAllActions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(getAllActionsUseCase.getAllActions(pageable));
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get all actions by user (paginated)",
            description = "Retrieves all actions performed by a specific user, using their userId. " +
                    "This action will also log that the requesting user accessed another user's actions."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user's actions",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<Page<ActionEntity>> getAllActionsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(getAllActionsByUserIdUseCase.getAllActionsByUserId(userId, pageable));
    }

}