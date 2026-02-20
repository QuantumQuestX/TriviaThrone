package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/games")
public class GameUploadController {
    private final Path storageDir = Paths.get("upload/games");

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadGame(
            @RequestParam("manifest") MultipartFile manifest,
            @RequestParam(value="metadata", required = false) MultipartFile metadata,
            @RequestParam(value="resources", required = false) MultipartFile[] resources,
            @AuthenticationPrincipal UserAccount user) {
        return null;
    }
}
