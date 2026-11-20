# Spec 005 — Integración continua

- Estado: Aprobada para planificar
- Rama: `spec/005-integracion-continua`
- Depende de: 001 (necesita que existan tests); se implementa antes de 003 y 004

## 1. Contexto y objetivo
Los agentes generan código más rápido de lo que una persona puede revisarlo línea por línea. La forma de confiar en un cambio no es leerlo todo: es que **pase siempre por el mismo camino automático de verificación** antes de integrarse. Objetivo: que ningún cambio llegue a `main` sin haber pasado los tests y los controles de seguridad.

## 2. Definiciones
| Término | Significado |
|---|---|
| **Cambio** | Un pull request (PR) contra `main`. |
| **Pipeline** | La secuencia automática de verificaciones que corre sobre cada cambio. |
| **Verde / rojo** | El pipeline terminó con todas las verificaciones aprobadas / con al menos una fallida. |
| **Umbral de merge** | Las condiciones mínimas para que un cambio se pueda integrar a `main`. |
| **Secreto** | Credencial, token o clave privada. |
| **Vulnerabilidad conocida** | Una dependencia con una vulnerabilidad publicada de severidad alta o crítica que ya tiene versión corregida. |

## 3. Comportamiento esperado
1. El pipeline corre automáticamente en **cada PR** contra `main` y en cada actualización del PR. También corre al integrar en `main`.
2. Verifica, en este orden (el más barato primero, para fallar rápido):
   1. Que no haya **secretos** en el código ni en el historial del PR.
   2. Que el código no tenga **patrones inseguros conocidos** (análisis estático de seguridad).
   3. Que las dependencias no tengan **vulnerabilidades conocidas**.
   4. Que pasen los **tests de backend**: unitarios, de integración con base de datos real y de arquitectura.
   5. Que pasen los **tests de frontend**.
   6. Que pasen los **tests E2E** contra la app levantada.
3. Si una verificación falla, el pipeline se detiene y queda en rojo, indicando cuál falló.
4. El reporte de los E2E (con las grabaciones de los que fallaron) queda **descargable** desde la ejecución.
5. **Umbral de merge** (queda escrito y configurado en GitHub):
   - Pipeline en verde.
   - Solo se integra por PR: no se hace push directo a `main`.
   - El humano revisó el PR y el `verify.md` de la spec correspondiente. Como hay un solo desarrollador, GitHub no le permite aprobar su propio PR, así que la revisión humana queda registrada como un comentario en el PR.
6. El pipeline usa permisos mínimos y las herramientas de terceros se fijan a una versión inmutable.

## 4. Casos borde
- PR que solo cambia documentación → el pipeline corre igual (es barato y mantiene la regla simple).
- Hallazgo de seguridad falso positivo → se puede excluir de forma explícita y comentada en la configuración de la herramienta, nunca desactivando la verificación entera.
- Vulnerabilidad sin versión corregida disponible → no bloquea, pero aparece en el log del pipeline.
- Test E2E inestable → hasta 2 reintentos automáticos. Si falla 3 veces, queda rojo.

## 5. Criterios de aceptación

| ID | Escenario | Entrada | Resultado esperado |
|---|---|---|---|
| 005-CA01 | Camino feliz | PR con cambios correctos | Pipeline verde con los 6 pasos ejecutados |
| 005-CA02 | Test unitario roto | PR que rompe un test de la spec 002 | Rojo en el paso de tests de backend; no corren los E2E |
| 005-CA03 | Secreto | PR que agrega un archivo con una clave privada o token de prueba | Rojo en el paso de secretos (el primero) |
| 005-CA04 | Patrón inseguro | PR con una consulta SQL armada concatenando texto del usuario | Rojo en el análisis estático |
| 005-CA05 | Dependencia vulnerable | PR que fija una dependencia en una versión con vulnerabilidad crítica conocida | Rojo en el paso de dependencias |
| 005-CA06 | E2E roto | PR que cambia un texto de la UI usado por un E2E | Rojo en E2E; el reporte con la grabación está descargable |
| 005-CA07 | Protección de `main` | Intento de push directo a `main` | GitHub lo rechaza |
| 005-CA08 | Merge bloqueado | PR con pipeline rojo | GitHub no permite integrarlo |

> Los criterios CA02 a CA06 se verifican con **PRs de prueba descartables** (se abren, se observa el rojo y se cierran sin integrar). El verificador deja los links en `verify.md`.

## 6. Fuera de alcance
- Despliegue continuo a un entorno real.
- Publicar imágenes de contenedor en un registro y escanearlas.
- Pruebas dinámicas de seguridad (DAST) contra la app en ejecución.
- Umbral mínimo de cobertura de código.
- Métricas DORA automatizadas.

## 7. Referencias
- [ADR-0006 — Pipeline de CI](../../adr/ADR-0006-pipeline-ci.md)
- [Guía de CI](../../catedra/ci.md)
- Versión inicial del pipeline: `.github/workflows/pipeline.yml`
