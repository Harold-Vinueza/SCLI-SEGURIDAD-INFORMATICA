package com.uteq.SCLI.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;

@Service
public class UploadService {

    // Carpeta base (fuera del jar). Se crea automáticamente si no existe.
    private final Path root = Paths.get("uploads/perfiles");

    /**
     * Guarda la foto de perfil y devuelve la URL pública (ej. /uploads/perfiles/estudiante/12.jpg).
     * @param tipo       subcarpeta (p.ej. "estudiante" / "docente")
     * @param idPersona  id de persona
     * @param file       archivo recibido
     */
    public String saveProfilePhoto(String tipo, Integer idPersona, MultipartFile file) throws Exception {
        if (idPersona == null) throw new IllegalArgumentException("idPersona requerido");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Archivo vacío");

        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "foto");
        String ext = getExtension(original);
        if (!List.of("jpg","jpeg","png").contains(ext))
            throw new IllegalArgumentException("Formato no permitido (solo JPG/PNG)");

        // Crea /uploads/perfiles/{tipo}
        Path dir = root.resolve(tipo);
        Files.createDirectories(dir);

        Path dest = dir.resolve(idPersona + "." + ext);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }

        // Devuelve ruta pública (servida por StaticResourceConfig)
        return "/uploads/perfiles/" + tipo + "/" + idPersona + "." + ext;
    }

    private String getExtension(String name) {
        int i = name.lastIndexOf('.');
        return (i >= 0) ? name.substring(i + 1).toLowerCase() : "jpg";
    }
}