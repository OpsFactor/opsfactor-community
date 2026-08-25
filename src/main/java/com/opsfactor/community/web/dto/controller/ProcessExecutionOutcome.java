package com.opsfactor.community.web.dto.controller;

/**
 * Semântica operacional da resposta HTTP devolvida por um disparo de task.
 *
 * <p>A mensagem humana continua no {@link ResponseDTO}, mas este enum permite
 * ao front distinguir uma execução já concluída da aceitação de trabalho que
 * ainda seguirá em fila ou worker.</p>
 */
public enum ProcessExecutionOutcome {

    /** A resposta só é devolvida depois de a execução síncrona terminar. */
    COMPLETED,

    /** O backend aceitou o comando, mas o processamento seguirá em background. */
    ACCEPTED_FOR_BACKGROUND_PROCESSING

}
