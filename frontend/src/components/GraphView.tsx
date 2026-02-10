// GraphView.tsx
import { useEffect, useRef } from 'react'
import * as d3 from 'd3'
import { GraphData, GraphNode, GraphEdge } from './GraphTypes'

type Props = {
  data: GraphData
  width?: number
  height?: number
}

export function GraphView({
  data,
  width = 800,
  height = 600,
}: Props) {
  const svgRef = useRef<SVGSVGElement | null>(null)

  useEffect(() => {
    if (!svgRef.current) return

    const svg = d3.select(svgRef.current)
    svg.selectAll('*').remove() // clear on re-render

    // --- simulation ---
    const simulation = d3
      .forceSimulation<GraphNode>(data.nodes)
      .force(
        'link',
        d3
          .forceLink<GraphNode, GraphEdge>(data.edges)
          .id(d => d.id)
          .distance(80)
          .strength(0.7)
      )
      .force('charge', d3.forceManyBody().strength(-200))
      .force('center', d3.forceCenter(width / 2, height / 2))

    // --- edges ---
    const link = svg
      .append('g')
      .attr('stroke', '#999')
      .attr('stroke-opacity', 0.6)
      .selectAll('line')
      .data(data.edges)
      .join('line')
      .attr('stroke-width', d => (d.score ? d.score * 2 : 1))

    // --- nodes ---
    const node = svg
      .append('g')
      .selectAll('circle')
      .data(data.nodes)
      .join('circle')
      .attr('r', d => (d.type === 'note' ? 12 : 6))
      .attr('fill', d => (d.type === 'note' ? '#3b82f6' : '#22c55e'))
      .call(
        d3
          .drag<SVGCircleElement, GraphNode>()
          .on('start', dragStarted)
          .on('drag', dragged)
          .on('end', dragEnded)
      )

    // --- labels ---
    const label = svg
      .append('g')
      .selectAll('text')
      .data(data.nodes)
      .join('text')
      .text(d => d.label)
      .attr('font-size', 10)
      .attr('dx', 14)
      .attr('dy', 4)

    simulation.on('tick', () => {
      link
        .attr('x1', d => (d.source as GraphNode).x!)
        .attr('y1', d => (d.source as GraphNode).y!)
        .attr('x2', d => (d.target as GraphNode).x!)
        .attr('y2', d => (d.target as GraphNode).y!)

      node
        .attr('cx', d => d.x!)
        .attr('cy', d => d.y!)

      label
        .attr('x', d => d.x!)
        .attr('y', d => d.y!)
    })

    function dragStarted(event: any, d: GraphNode) {
      if (!event.active) simulation.alphaTarget(0.3).restart()
      d.fx = d.x
      d.fy = d.y
    }

    function dragged(event: any, d: GraphNode) {
      d.fx = event.x
      d.fy = event.y
    }

    function dragEnded(event: any, d: GraphNode) {
      if (!event.active) simulation.alphaTarget(0)
      d.fx = null
      d.fy = null
    }

    return () => {
      simulation.stop()
    }
  }, [data, width, height])

  return <svg ref={svgRef} width={width} height={height} />
}
