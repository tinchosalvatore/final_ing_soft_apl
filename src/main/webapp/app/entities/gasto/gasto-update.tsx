import React, { useEffect } from 'react';
import { Button, Col, FormText, Row } from 'react-bootstrap';
import { Translate, ValidatedField, ValidatedForm, isNumber, translate } from 'react-jhipster';
import { Link, useNavigate, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getEntities as getCategorias } from 'app/entities/categoria/categoria.reducer';
import { getUsers } from 'app/modules/administration/user-management/user-management.reducer';

import { createEntity, getEntity, reset, updateEntity } from './gasto.reducer';

export const GastoUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const categorias = useAppSelector(state => state.categoria.entities);
  const users = useAppSelector(state => state.userManagement.users);
  const gastoEntity = useAppSelector(state => state.gasto.entity);
  const loading = useAppSelector(state => state.gasto.loading);
  const updating = useAppSelector(state => state.gasto.updating);
  const updateSuccess = useAppSelector(state => state.gasto.updateSuccess);

  const handleClose = () => {
    navigate(`/gasto${location.search}`);
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(id));
    }

    dispatch(getCategorias({}));
    dispatch(getUsers({}));
  }, []);

  useEffect(() => {
    if (updateSuccess) {
      handleClose();
    }
  }, [updateSuccess]);

  const saveEntity = values => {
    if (values.id !== undefined && typeof values.id !== 'number') {
      values.id = Number(values.id);
    }
    if (values.monto !== undefined && typeof values.monto !== 'number') {
      values.monto = Number(values.monto);
    }

    const entity = {
      ...gastoEntity,
      ...values,
      categoria: categorias.find(it => it.id.toString() === values.categoria?.toString()),
      user: users.find(it => it.id.toString() === values.user?.toString()),
    };

    if (isNew) {
      dispatch(createEntity(entity));
    } else {
      dispatch(updateEntity(entity));
    }
  };

  const defaultValues = () =>
    isNew
      ? {}
      : {
          ...gastoEntity,
          categoria: gastoEntity?.categoria?.id,
          user: gastoEntity?.user?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="trackerGastosApp.gasto.home.createOrEditLabel" data-cy="GastoCreateUpdateHeading">
            <Translate contentKey="trackerGastosApp.gasto.home.createOrEditLabel">Create or edit a Gasto</Translate>
          </h2>
        </Col>
      </Row>
      <Row className="justify-content-center">
        <Col md="8">
          {loading ? (
            <p>Loading...</p>
          ) : (
            <ValidatedForm mode="all" defaultValues={defaultValues()} onSubmit={saveEntity}>
              {!isNew && (
                <ValidatedField
                  name="id"
                  required
                  readOnly
                  id="gasto-id"
                  label={translate('global.field.id')}
                  validate={{ required: true }}
                />
              )}
              <ValidatedField
                label={translate('trackerGastosApp.gasto.monto')}
                id="gasto-monto"
                name="monto"
                data-cy="monto"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                  validate: v => isNumber(v) || translate('entity.validation.number'),
                }}
              />
              <ValidatedField
                label={translate('trackerGastosApp.gasto.fecha')}
                id="gasto-fecha"
                name="fecha"
                data-cy="fecha"
                type="date"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('trackerGastosApp.gasto.descripcion')}
                id="gasto-descripcion"
                name="descripcion"
                data-cy="descripcion"
                type="text"
                validate={{
                  maxLength: { value: 140, message: translate('entity.validation.maxlength', { max: 140 }) },
                }}
              />
              <ValidatedField
                id="gasto-categoria"
                name="categoria"
                data-cy="categoria"
                label={translate('trackerGastosApp.gasto.categoria')}
                type="select"
                required
              >
                <option value="" key="0" />
                {categorias
                  ? categorias.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.nombre}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <FormText>
                <Translate contentKey="entity.validation.required">This field is required.</Translate>
              </FormText>
              <ValidatedField id="gasto-user" name="user" data-cy="user" label={translate('trackerGastosApp.gasto.user')} type="select">
                <option value="" key="0" />
                {users
                  ? users.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.login}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <Button as={Link as any} id="cancel-save" data-cy="entityCreateCancelButton" to="/gasto" replace variant="info">
                <FontAwesomeIcon icon="arrow-left" />
                &nbsp;
                <span className="d-none d-md-inline">
                  <Translate contentKey="entity.action.back">Back</Translate>
                </span>
              </Button>
              &nbsp;
              <Button variant="primary" id="save-entity" data-cy="entityCreateSaveButton" type="submit" disabled={updating}>
                <FontAwesomeIcon icon="save" />
                &nbsp;
                <Translate contentKey="entity.action.save">Save</Translate>
              </Button>
            </ValidatedForm>
          )}
        </Col>
      </Row>
    </div>
  );
};

export default GastoUpdate;
