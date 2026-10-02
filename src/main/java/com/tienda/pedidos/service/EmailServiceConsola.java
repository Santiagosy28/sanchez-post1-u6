package com.tienda.pedidos.service;

import org.springframework.stereotype.Service;

// Implementacion de EmailService que imprime el correo en consola.
@Service
public class EmailServiceConsola implements EmailService {
    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        System.out.println("=== EMAIL ===");
        System.out.println("Para: " + destinatario);
        System.out.println("Asunto: " + asunto);
        System.out.println(cuerpo);
        System.out.println("=============");
    }
}
