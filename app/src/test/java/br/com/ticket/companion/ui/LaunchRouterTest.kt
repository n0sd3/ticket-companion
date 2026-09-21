package br.com.ticket.companion.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchRouterTest {
    @Test fun `a phone that only attends goes straight to the web and everyone else stays on the capture screens`() {
        assertEquals(Landing.WEB, LaunchRouter.landing(companionInUse = false, stayInApp = false, hasWebOrigin = true))
        // Quem já usou o Companion não é redirecionado.
        assertEquals(Landing.CAPTURE, LaunchRouter.landing(companionInUse = true, stayInApp = false, hasWebOrigin = true))
        // "Ajustes do app" aberto a partir do atendimento não pode voltar para o web em laço.
        assertEquals(Landing.CAPTURE, LaunchRouter.landing(companionInUse = false, stayInApp = true, hasWebOrigin = true))
        // Sem endereço salvo não há para onde ir: mostra o setup.
        assertEquals(Landing.CAPTURE, LaunchRouter.landing(companionInUse = false, stayInApp = false, hasWebOrigin = false))
    }
}
