import React from 'react';
import { Button, Modal } from 'react-bootstrap';

import { TEXTOS } from './textos';

interface Props {
  abierto: boolean;
  onConfirmar: () => void;
  onCancelar: () => void;
}

/** Confirmación de borrado (spec 001 §5.1: diálogo con el botón "Confirmar"). */
export const GastoBorrarDialog = ({ abierto, onConfirmar, onCancelar }: Props) => (
  <Modal show={abierto} onHide={onCancelar} aria-labelledby="gasto-borrar-titulo">
    <Modal.Header closeButton>
      <Modal.Title id="gasto-borrar-titulo">{TEXTOS.borrarTitulo}</Modal.Title>
    </Modal.Header>
    <Modal.Body>{TEXTOS.confirmarBorrado}</Modal.Body>
    <Modal.Footer>
      <Button variant="secondary" onClick={onCancelar}>
        {TEXTOS.cancelar}
      </Button>
      <Button variant="danger" onClick={onConfirmar}>
        {TEXTOS.confirmar}
      </Button>
    </Modal.Footer>
  </Modal>
);

export default GastoBorrarDialog;
