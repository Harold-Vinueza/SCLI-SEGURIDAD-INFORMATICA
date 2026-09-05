package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.StudentProfileDTO;
import com.uteq.SCLI.repository.StudentProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service endurecido:
 * - No lanza excepciones salvo IllegalArgumentException (formato inválido).
 * - Registra en logs y continúa cuando sea seguro hacerlo.
 */
@Service
public class StudentProfileService {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileService.class);

    private final StudentProfileRepository repo;
    private final UploadService uploadService;

    public StudentProfileService(StudentProfileRepository repo, UploadService uploadService) {
        this.repo = repo;
        this.uploadService = uploadService;
    }

    public StudentProfileDTO get(int idPersona){
        return repo.findByIdPersona(idPersona);
    }

    /**
     * Guarda cambios de perfil. Solo puede lanzar IllegalArgumentException cuando
     * el archivo NO cumple formato (JPG/PNG), lo cual queremos mostrar al usuario.
     * Cualquier otro error se registra y NO se relanza para evitar falsos "errores" en UI
     * cuando parte (o todo) ya fue persistido correctamente.
     */
    public void save(StudentProfileDTO d, MultipartFile foto) {
        // --- 1) Foto (opcional) ---
        if (foto != null && !foto.isEmpty()) {
            try {
                String url = uploadService.saveProfilePhoto("estudiante", d.getIdPersona(), foto);
                d.setFotoUrl(url);
            } catch (IllegalArgumentException ex) {
                // Formato o validación de UploadService -> relanzamos para que UI lo muestre
                throw ex;
            } catch (Exception ex) {
                // Cualquier otro problema al guardar la foto: logueamos y seguimos
                log.warn("No se pudo guardar la foto de perfil (continuando sin foto). idPersona={} msg={}",
                        d.getIdPersona(), ex.getMessage());
            }
        }

        // --- 2) Persona ---
        try {
            repo.updatePersona(d);
        } catch (Exception ex) {
            // Si falla aquí, sí es crítico (persona no actualizada). Registramos y relanzamos para que UI avise.
            log.error("Error actualizando PERSONA idPersona={}", d.getIdPersona(), ex);
            throw ex;
        }

        // --- 3) Resolver id_carrera por nombre (opcional) ---
        try {
            if (d.getIdCarrera() == null && d.getCarreraNombre() != null && !d.getCarreraNombre().isBlank()) {
                Integer idCar = repo.findCarreraIdByNombre(d.getCarreraNombre());
                d.setIdCarrera(idCar); // puede quedar null si no existe
            }
        } catch (Exception ex) {
            // No es crítico: el UPSERT seguirá con el id_carrera actual/null
            log.warn("No se pudo resolver id_carrera desde nombre='{}' (continuando). idPersona={} msg={}",
                    d.getCarreraNombre(), d.getIdPersona(), ex.getMessage());
        }

        // --- 4) Estudiante (UPSERT) ---
        try {
            repo.upsertEstudiante(d);
        } catch (Exception ex) {
            // Esto sí es importante (datos académicos), pero si ya actualizamos persona,
            // preferimos registrar y relanzar para que UI informe.
            log.error("Error guardando ESTUDIANTE (UPSERT) idPersona={}", d.getIdPersona(), ex);
            throw ex;
        }
    }

    public StudentProfileRepository.Result cambiarClave(int idUsuario, String actual, String nueva){
        return repo.cambiarClave(idUsuario, actual, nueva);
    }
}