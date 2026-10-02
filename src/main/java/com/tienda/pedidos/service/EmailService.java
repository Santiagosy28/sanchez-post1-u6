package com.tienda.pedidos.service;

// Interfaz simple de envio de correo. Se implementa con una version de consola
// para no depender de un servidor SMTP real durante el laboratorio.
public interface EmailService {
    void enviar(String destinatario, String asunto, String cuerpo);
}
