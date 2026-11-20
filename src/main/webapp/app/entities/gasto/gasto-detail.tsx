import React, { useEffect } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { Link, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_LOCAL_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './gasto.reducer';

export const GastoDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id!));
  }, []);

  const gastoEntity = useAppSelector(state => state.gasto.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="gastoDetailsHeading">
          <Translate contentKey="trackerGastosApp.gasto.detail.title">Gasto</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{gastoEntity.id}</dd>
          <dt>
            <span id="monto">
              <Translate contentKey="trackerGastosApp.gasto.monto">Monto</Translate>
            </span>
          </dt>
          <dd>{gastoEntity.monto}</dd>
          <dt>
            <span id="fecha">
              <Translate contentKey="trackerGastosApp.gasto.fecha">Fecha</Translate>
            </span>
          </dt>
          <dd>{gastoEntity.fecha ? <TextFormat value={gastoEntity.fecha} type="date" format={APP_LOCAL_DATE_FORMAT} /> : null}</dd>
          <dt>
            <span id="descripcion">
              <Translate contentKey="trackerGastosApp.gasto.descripcion">Descripcion</Translate>
            </span>
          </dt>
          <dd>{gastoEntity.descripcion}</dd>
          <dt>
            <Translate contentKey="trackerGastosApp.gasto.categoria">Categoria</Translate>
          </dt>
          <dd>{gastoEntity.categoria ? gastoEntity.categoria.nombre : ''}</dd>
          <dt>
            <Translate contentKey="trackerGastosApp.gasto.user">User</Translate>
          </dt>
          <dd>{gastoEntity.user ? gastoEntity.user.login : ''}</dd>
        </dl>
        <Button as={Link as any} to="/gasto" replace variant="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button as={Link as any} to={`/gasto/${gastoEntity.id}/edit`} replace variant="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default GastoDetail;
