import React from 'react';
import { NavItem, NavLink } from 'react-bootstrap';
import { NavLink as Link } from 'react-router';

import { faChartPie, faList, faTags } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { TEXTOS } from './textos';

/** Ítem de menú "Mis gastos" (spec 001, regla 14). */
export const MenuMisGastos = () => (
  <NavItem>
    <NavLink as={Link} to="/mis-gastos" className="d-flex align-items-center">
      <FontAwesomeIcon icon={faList} />
      <span>{TEXTOS.menu}</span>
    </NavLink>
  </NavItem>
);

/** Ítem de menú "Resumen" (spec 002, punto 7), al lado de "Mis gastos" para todo usuario con sesión. */
export const MenuResumen = () => (
  <NavItem>
    <NavLink as={Link} to="/resumen" className="d-flex align-items-center">
      <FontAwesomeIcon icon={faChartPie} />
      <span>Resumen</span>
    </NavLink>
  </NavItem>
);

/** Ítem de menú "Categorías", solo para ADMIN (spec 001, regla 14). Usa el CRUD generado de categorías. */
export const MenuCategorias = () => (
  <NavItem>
    <NavLink as={Link} to="/categoria" className="d-flex align-items-center">
      <FontAwesomeIcon icon={faTags} />
      <span>Categorías</span>
    </NavLink>
  </NavItem>
);
