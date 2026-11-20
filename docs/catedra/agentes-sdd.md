# Guía: cómo trabajamos con agentes (SDD)

Responsable: todos los roles. La lee cualquier agente antes de su primera tarea en el repo.

## 1. Qué se aplica
| Práctica | Cómo se concreta en este repo |
|---|---|
| Fuente única de verdad del modelo | `tracker.jh`; el código del modelo es una salida del generador |
| Instrucciones globales | `AGENTS.md` (producto, territorios, comandos, reglas, mapa, criterios). `CLAUDE.md` solo lo importa |
| Context engineering / recuperación bajo demanda | `AGENTS.md` es corto y apunta a los documentos; cada agente lee solo lo que su tarea necesita |
| Sesiones nuevas por tarea | Cada rol y cada spec arranca en una sesión limpia; el plan y el progreso viven en archivos, no en el chat |
| Ciclo agéntico con aprobación entre etapas | Especificar → Planificar → Tareas → Implementar → Verificar (sección 2) |
| Specs con el formato de la cátedra | `docs/specs/NNN-*/spec.md`, con las 6 secciones y al menos 5 criterios de aceptación |
| ADRs | `docs/adr/`, formato Contexto / Decisión / Consecuencias / Alternativas |
| Territorios y needles de JHipster | `AGENTS.md` §2 |
| Tests escritos por otro agente | El rol **tester** escribe los tests antes que el **implementador** escriba el código |
| Agente revisor sin participación previa | El rol **verificador** produce `verify.md` |
| Autonomía según qué tan fácil es deshacer | Ver sección 4 |
| Sin secretos en el contexto | Regla 4 de `AGENTS.md`; Gitleaks en el CI |

## 2. El ciclo por cada spec

| Etapa | Quién | Produce | Gate (lo aprueba el humano) |
|---|---|---|---|
| 1. Especificar | documentador + humano | `spec.md` (+ ADRs) | Spec revisada: no queda nada librado a interpretación |
| 2. Tests | agente **testing** | Tests que fallan, uno o más por criterio | Cada criterio tiene un test; los tests compilan y están en **rojo** |
| 3. Planificar | agente del tema (coder, ci, pwa, observabilidad) | `plan.md` | Plan aprobado: archivos, ubicación de reglas, decisiones |
| 4. Tareas | agente del tema | `tasks.md` | — (se revisa junto con el plan) |
| 5. Implementar | agente del tema | Código + commits chicos en `spec/NNN-*` | Tests en verde y CI en verde |
| 6. Verificar | agente **seguridad** (no programó nada) | `verify.md` | El humano lee `verify.md` y el PR, y decide el merge |

**Si un agente falla, primero se revisa la spec:** casi siempre falta algo ahí. Se corrige la spec (en el mismo PR) y se vuelve a la etapa que corresponda.

### Secuencia completa de agentes
| # | Agente | Qué hace |
|---|---|---|
| 1 | coder | Paso 000: genera la app desde `tracker.jh` |
| 2 | testing | Tests de 001 y 002 en rojo (TDD) |
| 3 | coder | Plan + implementación de 001 y después de 002 hasta poner esos tests en verde |
| 4 | seguridad | `verify.md` de 000, 001 y 002 |
| 5 | ci | Spec 005 (desde acá, todo entra por PR con el pipeline andando) |
| 6 | testing → pwa | E2E de 003 en rojo → implementación de la PWA |
| 7 | observabilidad | Spec 004 (con sus tests de integración); testing graba los E2E si hacen falta |
| 8 | seguridad | `verify.md` de 003, 004 y 005; auditoría final de S1 a S10 y 12 factors |

Una sesión por agente con nombre propio, que el humano después usa para estudiar ese tema. Si una sesión se llena de contexto (*context rot*), se abre otra con el mismo nombre que retoma desde `tasks.md` y `verify.md`.

## 3. Paso 000: generación base (sin spec)
No tiene lógica de negocio: es la salida determinista de `tracker.jh`. Lo hace el agente **coder** en la rama `spec/000-base`:
1. Verificar los requisitos de `AGENTS.md` §3 (en especial la versión de Node).
2. `npx generator-jhipster@9.4.0 jdl tracker.jh` en la raíz del repo. Si JHipster pregunta por conflictos en `.gitignore`, `AGENTS.md`, `CLAUDE.md` o `docs/`, responder `n`: son nuestros.
3. Volver a agregar al `.gitignore` generado las líneas propias (`docs/enunciado.md`, `docs/defense/`, `.env`), si se perdieron.
4. `./mvnw -ntp verify` y `npm test` en verde.
5. Actualizar la tabla de comandos de `AGENTS.md` con los scripts reales que generó JHipster (por ejemplo, el nombre del script de E2E).
6. Aplicar la configuración base de 12 factors (`12-factors.md` §1, factores III, VII y IX) y la de seguridad (`seguridad.md` §2, puntos S5 y S6). Crear `.env.example`.
7. Commit: `chore: generación base desde tracker.jh` y PR.

## 4. Autonomía de los agentes
Criterio: **qué tan difícil es deshacer lo que hace**, no qué tan difícil es la tarea.

| Acción | Modo |
|---|---|
| Editar archivos en una rama `spec/*` | Automático (se descarta la rama si sale mal) |
| Correr builds y tests | Automático |
| Regenerar con JHipster | Con confirmación (pisa archivos y tiene conflictos) |
| `git push`, abrir PR | Con confirmación |
| Merge a `main`, cambios de configuración en GitHub, borrar ramas | Solo el humano |
| Instalar herramientas globales, tocar fuera del repo | Solo con permiso explícito |

## 5. Cómo arrancar una sesión de agente (plantilla de pedido)
```
Sos el agente <coder|testing|ci|pwa|observabilidad|seguridad> del repo. Leé AGENTS.md y después solo lo que necesites.
Tarea: <etapa> de la spec NNN (docs/specs/NNN-*/spec.md).
Rama: spec/NNN-*. Al terminar: <artefacto que produce>.
No hagas: <lo que el rol no toca, según AGENTS.md §6>.
```

## 6. Qué queda afuera
- GitHub Spec Kit como herramienta (se usa su idea de etapas, no sus comandos).
- Agentes corriendo en paralelo sobre la misma rama.
- Devcontainers: el entorno de desarrollo es la máquina local con Docker.

## 7. Referencias
`AGENTS.md`, `docs/specs/_plantillas/`, `docs/adr/README.md`, `docs/trazabilidad.md`.
