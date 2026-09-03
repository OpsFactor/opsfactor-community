package com.opsfactor.community.web.restcontroller.masterdata;

import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.CharacteristicCatalogService;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaLocationDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaProdutoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Shared characteristic lookup endpoints used by Planning Book and User View selectors. */
@RestController
public class CharacteristicCatalogRestController {

    /** Service de consulta ao catalogo de caracteristicas Community. */
    @Autowired
    private CharacteristicCatalogService characteristicCatalogService;

    /** Lista as caracteristicas de material disponiveis aos seletores. */
    @GetMapping("api/secured/material/characteristics")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<CaracteristicaProdutoDTO>> getMaterialCharacteristics() {

        return ResponseEntity.ok(characteristicCatalogService.getCaracteristicaProdutoDTOList());

    }

    /** Lista as caracteristicas de location disponiveis aos seletores. */
    @GetMapping("api/secured/location/characteristics")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<CaracteristicaLocationDTO>> getLocationCharacteristics() {

        return ResponseEntity.ok(characteristicCatalogService.getCaracteristicaLocationDTOList());

    }

}
